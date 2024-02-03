package com.example.webflux.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
public class SampleController {

    @GetMapping("/sample/hello")
    public Mono<String> getHello() {

        //rearctor
        // publisher <---> subscriber  교환
        //

        return Mono.just("hello rest controller with webflux");
    }


}
