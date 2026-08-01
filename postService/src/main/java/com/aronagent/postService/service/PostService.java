package com.aronagent.postService.service;

import com.aronagent.postService.dto.PostCreateRequestDto;
import com.aronagent.postService.dto.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PostService {
    PostDto createPost(PostCreateRequestDto requestDto);
    PostDto getPostById(Long postId);
    List<PostDto> getAllPostsOfUser(Long userId);
    void deletePost(Long id);
    Page<PostDto> getAllPosts(Pageable pageable); // new

}