package com.example.webflux.dto.response;

import com.example.webflux.domain.Post;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserPostResponse {

    private Long id;
    private String username;
    private String title;
    private String content;
    private LocalDateTime createAt;
    private LocalDateTime updatedAt;

    public static UserPostResponse of(Post post) {
        return UserPostResponse.builder()
                .id(post.getId())
                .username("TODO")
                .title(post.getTitle())
                .content(post.getContent())
                .createAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
