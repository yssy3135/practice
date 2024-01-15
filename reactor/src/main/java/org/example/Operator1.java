package org.example;

import reactor.core.publisher.Flux;

public class Operator1 {

    //map
    public Flux<Integer> fluxMap() {
        return Flux.range(1,5)
                .map(i -> i * 2)
                .log();
    }

    // filter
    public Flux<Integer> fluxFilter() {
        return Flux.range(1, 10)
                .filter(i ->  i > 5)
                .log();
    }
    // take
    public Flux<Integer> fluxFilterTake() {
        return Flux.range(1, 10)
                .filter(i -> i > 5)
                .take(3)
                .log();
    }

    // flatmap
    // 비동기이고 순서가 보장되지 않는다.
    public Flux<Integer> fluxFlatMap() {
        return Flux.range(1, 10)
                .flatMap(i -> Flux.range(i*10, 10))
                .log();
    }


    public Flux<Integer> fluxFlatMap2() {
        return Flux.range(1, 9)
                .flatMap(i -> Flux.range(1, 9)
                        .map(j -> {
                            System.out.printf("%d * %d = %d\n", i, j, i * j);
                            return i * j;
                        })
                )

                .log();
    }


}
