package com.example.webflux.repository;

import com.example.webflux.domain.Post;
import reactor.core.publisher.Flux;

public interface PostCustomR2dbcRepository {

    Flux<Post> findAllByUserId(Long userId);



}
