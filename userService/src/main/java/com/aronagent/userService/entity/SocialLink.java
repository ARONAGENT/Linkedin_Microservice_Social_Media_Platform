package com.aronagent.userService.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SocialLink {
    private String platform; // e.g. "GitHub", "LinkedIn", "Twitter"
    private String url;
}