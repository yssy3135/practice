package com.example.webflux.repository;

import com.example.webflux.domain.Post;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PostR2dbcRepository extends ReactiveCrudRepository<Post, Long>, PostCustomR2dbcRepository {

    @Query("SELECT p from Post p WHERE p.userId = :userId ")
    Flux<Post> findByUserId(Long userId);
}
