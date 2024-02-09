package com.example.webflux.repository;

import com.example.webflux.domain.Post;
import com.example.webflux.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;

@Repository
@RequiredArgsConstructor
public class PostCustomR2dbcRepositoryImpl implements PostCustomR2dbcRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Flux<Post> findAllByUserId(Long userId) {
        var sql = """
                SELECT p.id as pid, p.user_id as userId, p.title, p.content, p.createed_at as createdAt, p.updated_at as updatedAt,
                u.id as uid, u.name asname, u.email as email, u.created_at as uCreatedAt, u.updated_at as uUpdatedAt 
                FROM posts p 
                left join users u on p.userId = u.id 
                WHERE p.user = :userId
                """;

        databaseClient.sql(sql)
                .bind((String) "userId", userId)
                .fetch()
                .all()
                .map(row -> Post.builder()
                        .id((Long) row.get("pid"))
                        .userId((Long) row.get("userId"))
                        .title((String) row.get("title"))
                        .content((String) row.get("content"))
                        .user(
                                User.builder()
                                        .id((Long) row.get("id"))
                                        .name((String) row.get("name") )
                                        .email((String) row.get("email"))
                                        .createdAt(((ZonedDateTime) row.get("uCreatedAt")).toLocalDateTime())
                                        .updatedAt(((ZonedDateTime) row.get("uUpdatedAt")).toLocalDateTime())
                                        .build()
                        )
                        .createdAt(((ZonedDateTime) row.get("createdAt")).toLocalDateTime())
                        .updatedAt(((ZonedDateTime) row.get("updatedAt")).toLocalDateTime())
                        .build());



        return null;
    }
}
