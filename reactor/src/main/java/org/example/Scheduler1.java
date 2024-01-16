package org.example;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class Scheduler1 {


    // boundedElastic 을 사용할 것이다.
    public Flux<Integer> fluxMapWithSubscribeOn() {
        return Flux.range(1, 10)
                .map(i -> i * 2)
                .subscribeOn(Schedulers.boundedElastic())
                .log();
    }


    // subscribeOn은 전체 구문
    // publishOn 이 구문을 사용한 다음부터 boundedElastic을 사용하겠다.
    // 별로의 스레드에서 실행하는 스케줄러
    public Flux<Integer> fluxMapWithPublishOn() {
        return Flux.range(1, 10)
                .map(i -> i + 1)
                .publishOn(Schedulers.boundedElastic())
                .log()
                .publishOn(Schedulers.parallel())
                .log()
                .map(i -> i * 2);
    }
}
