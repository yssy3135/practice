package com.example.webflux.controller;

import com.example.webflux.dto.response.PostResponse;
import com.example.webflux.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    @GetMapping("/{id}")
    public Mono<PostResponse> getPostContent(@PathVariable Long id) {
        return postService.getPostContent(id);
    }


    @GetMapping("/search")
    public Flux<PostResponse> getMultiplePostContent(@RequestParam(name = "idList") List<Long> idList) {
        return postService.getMultiplePostContent(idList);
    }

}
