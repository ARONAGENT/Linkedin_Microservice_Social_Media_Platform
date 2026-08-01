package com.aronagent.userService.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private Long userId; // links to User.id, no FK — separate service

    private String profileImageUrl;   // like LinkedIn profile pic
    private String bannerImageUrl;    // like LinkedIn cover/banner


    private LocalDate dateOfBirth;

    @Column(length = 2000)
    private String about;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_social_links", joinColumns = @JoinColumn(name = "profile_id"))
    private List<SocialLink> socialLinks;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill")
    private List<String> skills;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_projects", joinColumns = @JoinColumn(name = "profile_id"))
    private List<Project> projects;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "profile_certificates", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "certificate_url", length = 1000)
    private List<String> certificateUrls; // google docs links

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}