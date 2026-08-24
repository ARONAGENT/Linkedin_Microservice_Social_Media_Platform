package com.aronagent.postService.service;

import com.aronagent.postService.auth.AuthContextHolder;
import com.aronagent.postService.client.ConnectionsServiceClient;
import com.aronagent.postService.client.UploaderServiceClient;
import com.aronagent.postService.client.UserServiceClient;
import com.aronagent.postService.dto.PersonDto;
import com.aronagent.postService.dto.PostCreateRequestDto;
import com.aronagent.postService.dto.PostDto;
import com.aronagent.postService.dto.UserSummaryDto;
import com.aronagent.postService.entity.Post;
import com.aronagent.postService.event.PostCreated;
import com.aronagent.postService.exception.BadRequestException;
import com.aronagent.postService.repository.PostRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserServiceClient userServiceClient;
    private final ModelMapper modelMapper;
    private final UploaderServiceClient uploaderServiceClient;
    private final ConnectionsServiceClient connectionsServiceClient;
    private final KafkaTemplate<Long, PostCreated> postCreatedKafkaTemplate;


    @Override
    @CircuitBreaker(name = "createPostCB", fallbackMethod = "createPostFallback")
    public PostDto createPost(PostCreateRequestDto requestDto) {
        Long userId = AuthContextHolder.getCurrentUserId(); // never trust userId from the request body

        Boolean exists = userServiceClient.userExists(userId);
        if (exists == null || !exists) {
            throw new BadRequestException("User account not found");
        }

        List<String> imageurls = uploadImagesIfPresent(requestDto.getFiles());

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(requestDto.getContent());
        post.setImageUrls(imageurls); // adjust to however you actually populate this

        List<PersonDto> personDtoList = connectionsServiceClient.getFirstDegreeConnections();


        post = postRepository.save(post);

        for(PersonDto person: personDtoList) { // send notification to each connection
            PostCreated postCreated = PostCreated.builder()
                    .postId(post.getId())
                    .content(post.getContent())
                    .userId(person.getUserId())
                    .ownerUserId(userId)
                    .build();
            postCreatedKafkaTemplate.send("post_created_topic", postCreated);
        }
        return enrich(post);
    }

    /**
     * Only calls uploader-service when there's actually something to upload.
     * Calling it with an empty/null file list sends an empty multipart body,
     * which uploader-service correctly rejects with 400 since it requires
     * a non-empty "files" part — this was causing every text-only post to fail.
     */
    private List<String> uploadImagesIfPresent(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        // Guard against a list that technically has entries but they're all
        // empty placeholder parts (e.g. an <input type="file"> submitted with
        // nothing selected can sometimes still produce an empty MultipartFile).
        List<MultipartFile> nonEmptyFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());

        if (nonEmptyFiles.isEmpty()) {
            return Collections.emptyList();
        }

        return uploaderServiceClient.uploadMultipleImages(nonEmptyFiles);
    }

    @Override
    @Retry(
            name = "getPostByIdRetry",
            fallbackMethod = "getPostByIdFallback"
    )
    public PostDto getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BadRequestException("Post not found"));
        return enrich(post);
    }

    @Override
    @Retry(
            name = "getAllPostsOfUserRetry",
            fallbackMethod = "getAllPostsOfUserFallback"
    )
    public List<PostDto> getAllPostsOfUser(Long userId) {
        List<Post> posts = postRepository.findByUserId(userId);
        return enrichAll(posts);
    }

    @Override
    public void deletePost(Long id) {
        Long currentUserId = AuthContextHolder.getCurrentUserId();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Post not found"));

        if (!post.getUserId().equals(currentUserId)) {
            throw new BadRequestException("You can only delete your own posts");
        }

        postRepository.delete(post);
    }

    @Override
    @Retry(
            name = "getAllPostsRetry",
            fallbackMethod = "getAllPostsFallback"
    )
    public Page<PostDto> getAllPosts(Pageable pageable) {
        Page<Post> postPage = postRepository.findAll(pageable);

        if (postPage.isEmpty()) {
            return postPage.map(post -> modelMapper.map(post, PostDto.class));
        }

        List<Long> userIds = postPage.getContent().stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserSummaryDto> userMap = userServiceClient.getUserSummaries(userIds).stream()
                .collect(Collectors.toMap(UserSummaryDto::getUserId, u -> u));

        return postPage.map(post -> {
            PostDto dto = modelMapper.map(post, PostDto.class);
            UserSummaryDto u = userMap.get(post.getUserId());
            if (u != null) {
                dto.setName(u.getName());
                dto.setProfileUrl(u.getProfileImageUrl());
            }
            return dto;
        });
    }

    // ---------- helpers ----------

    private PostDto enrich(Post post) {
        UserSummaryDto user = userServiceClient.getUserSummary(post.getUserId());
        PostDto dto = modelMapper.map(post, PostDto.class);
        if (user != null) {
            dto.setName(user.getName());
            dto.setProfileUrl(user.getProfileImageUrl());
        }
        return dto;
    }

    private List<PostDto> enrichAll(List<Post> posts) {
        if (posts.isEmpty()) return List.of();

        List<Long> userIds = posts.stream()
                .map(Post::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserSummaryDto> userMap = userServiceClient.getUserSummaries(userIds).stream()
                .collect(Collectors.toMap(UserSummaryDto::getUserId, u -> u));

        return posts.stream().map(post -> {
            PostDto dto = modelMapper.map(post, PostDto.class);
            UserSummaryDto u = userMap.get(post.getUserId());
            if (u != null) {
                dto.setName(u.getName());
                dto.setProfileUrl(u.getProfileImageUrl());
            }
            return dto;
        }).collect(Collectors.toList());
    }


    // All FallBack Methods For Posts
    public PostDto getPostByIdFallback(Long postId, Throwable throwable) {
        log.error("Failed to get post with id {} after retries. Reason: {}", postId, throwable.getMessage());
        throw new RuntimeException("Unable to fetch post at the moment. Please try again later.");
    }

    public List<PostDto> getAllPostsOfUserFallback(Long userId, Throwable throwable) {
        log.error("Failed to get posts for user {} after retries. Reason: {}", userId, throwable.getMessage());
        throw new RuntimeException("Unable to fetch user posts at the moment. Please try again later.");
    }

    public Page<PostDto> getAllPostsFallback(Pageable pageable, Throwable throwable) {
        log.error("Failed to get all posts after retries. Reason: {}", throwable.getMessage());
        throw new RuntimeException("Unable to fetch posts at the moment. Please try again later.");
    }

    public PostDto createPostFallback(PostCreateRequestDto requestDto, Throwable throwable) {
        log.error("Failed to create post. Reason: {}", throwable.getMessage());
        throw new RuntimeException("Unable to create post at the moment. Please try again later.");
    }
}