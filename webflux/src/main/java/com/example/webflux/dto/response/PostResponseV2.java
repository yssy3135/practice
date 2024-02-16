package com.example.webflux.dto.response;

import com.example.webflux.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostResponseV2 {

    private Long id;

    private Long userId;

    private String title;

    private String content;

    private LocalDateTime createAt;

    private LocalDateTime updatedAt;

    public static PostResponseV2 of(Post post) {
        return PostResponseV2.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .createAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

}
