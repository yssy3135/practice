package org.example;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class Publisher {

    Flux<Integer> startFlux() {
        return Flux.range(1, 10).log();
    }

    Flux<String> startFluxList() {
        return Flux.fromIterable(List.of("a", "b", "c", "d"));
    }

    public Mono<Integer> startMono() {
        return Mono.just(1).log();
    }

    public Mono<?> startMonoEmpty() {
        return Mono.empty().log();
    }

    public Mono<?> startMonoException() {
        return Mono.error(new Exception("hello reactor"));
    }

}
