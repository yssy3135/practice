package com.example.webflux.controller;

import com.example.webflux.dto.response.PostResponseV2;
import com.example.webflux.dto.request.PostCreateRequest;
import com.example.webflux.service.PostServiceV2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v2/posts")
@RequiredArgsConstructor
public class PostControllerV2 {

    private final PostServiceV2 postServiceV2;

    @PostMapping("")
    public Mono<PostResponseV2> createPost(@RequestBody PostCreateRequest request) {
        return postServiceV2.create(request.getUserId(), request.getTitle(), request.getContent())
                .map(PostResponseV2::of);
    }

    @GetMapping("")
    public Flux<PostResponseV2> findAll() {
        return postServiceV2.findAll().map(PostResponseV2::of);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PostResponseV2>> findById(@PathVariable Long id) {
        return postServiceV2.findById(id)
                .map(p -> ResponseEntity.ok().body(PostResponseV2.of(p)))
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<?>> deleteById(@PathVariable Long id) {
        return postServiceV2.deleteById(id).then(Mono.just(ResponseEntity.noContent().build()));
    }


}
