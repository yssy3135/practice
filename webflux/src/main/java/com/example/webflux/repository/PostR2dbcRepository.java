package com.example.webflux.repository;

import com.example.webflux.domain.Post;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PostR2dbcRepository extends ReactiveCrudRepository<Post, Long> {

}
