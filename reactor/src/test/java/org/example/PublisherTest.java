package org.example;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class PublisherTest {

    private Publisher publisher = new Publisher();

    @Test
    void startFlux() {
        StepVerifier.create(publisher.startFlux())
                .expectNext(1,2,3,4,5,6,7,8,9,10)
                .verifyComplete();
    }

    @Test
    public void startMono() {
        StepVerifier.create(publisher.startMono())
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    public void startMonoEmpty() {
        StepVerifier.create(publisher.startMonoEmpty())
                .verifyComplete();
    }

    @Test
    void startMonoException() {
        StepVerifier.create(publisher.startMonoException())
                .expectError(Exception.class)
                .verify();
    }

    @Test
    public void startFluxList() {
        StepVerifier.create(publisher.startFluxList())
                .expectNext("a", "b", "c", "d")
                .verifyComplete();
    }
}