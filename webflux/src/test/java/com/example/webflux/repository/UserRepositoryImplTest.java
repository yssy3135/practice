package com.example.webflux.repository;

import com.example.webflux.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryImplTest {
    private UserRepository userRepository ;

    @BeforeEach
    public void before() {
        userRepository = new UserRepositoryImpl();
    }

    @Test
    void save() {
        User user = User.builder().name("greg").email("greg@test.com").build();
        StepVerifier.create(userRepository.save(user))
                .assertNext(u -> {
                    assertEquals(1L, u.getId());
                    assertEquals("greg", u.getName());
                })
                .verifyComplete();
    }

    @Test
    void findAll() {
        userRepository.save(User.builder().name("greg").email("greg@test.com").build());
        userRepository.save(User.builder().name("greg2").email("greg2@test.com").build());
        userRepository.save(User.builder().name("greg3").email("greg3@test.com").build());

        StepVerifier.create(userRepository.findAll())
                .expectNextCount(3L)
                .verifyComplete();
    }

    @Test
    void findById() {
        userRepository.save(User.builder().name("greg").email("greg@test.com").build());
        userRepository.save(User.builder().name("greg2").email("greg2@test.com").build());

        StepVerifier.create(userRepository.findById(2L))
                .assertNext(u -> {
                    assertEquals(2L, u.getId());
                    assertEquals("greg2", u.getName());
                })
                .verifyComplete();
    }

    @Test
    void deleteById() {
        userRepository.save(User.builder().name("greg").email("greg@test.com").build());
        userRepository.save(User.builder().name("greg2").email("greg2@test.com").build());

        StepVerifier.create(userRepository.deleteById(1L))
                .expectNext(1)
                .verifyComplete();



    }
}