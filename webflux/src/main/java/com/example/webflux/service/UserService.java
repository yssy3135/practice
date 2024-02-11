package com.example.webflux.service;

import com.example.webflux.config.RedisConfig;
import com.example.webflux.domain.User;
import com.example.webflux.repository.UserR2dbcRepository;
import com.example.webflux.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserService {

//    private final UserRepository userRepository;
    private final UserR2dbcRepository userR2dbcRepository;

    private final ReactiveRedisTemplate<String, User> reactiveRedisTemplate;


    public Mono<User> create(String name, String email) {

        return userR2dbcRepository.save(User.builder().name(name).email(email).build());
    }

    public Flux<User> findAll() {
        return userR2dbcRepository.findAll();
    }

    private String getUserCacheKey(Long id) {
        return "user:%d".formatted(id);
    }

    public Mono<User> findById(Long id) {
        // 1. redis 조회
        // 2. 값이 존재하면 응답을 하고
        // 3. 없으면 DB에 질의하고 그 결과를 redis에 저장
        return reactiveRedisTemplate.opsForValue()
                .get(getUserCacheKey(id))
                .switchIfEmpty(
                    userR2dbcRepository.findById(id)
                            .flatMap(user ->
                                reactiveRedisTemplate.opsForValue()
                                        .set(getUserCacheKey(id), user, Duration.ofSeconds(30))
                                        .then(Mono.just(user))
                            )
                );
    }

    public Mono<Void> deleteById(Long id) {
        return userR2dbcRepository.deleteById(id)
                .then(reactiveRedisTemplate.unlink(getUserCacheKey(id)))
                .then(Mono.empty());
    }

    public Mono<Void> deleteByName(String name) {
        return userR2dbcRepository.deleteByName(name);
    }

    public Mono<User> update(Long id, String name, String email) {
        //empty를 처리할 수 있는 방법
        // switchIfEmpty, defaultIfEmpty

        return userR2dbcRepository.findById(id)
                .flatMap(u -> {
                    u.setName(name);
                    u.setEmail(email);
                    return userR2dbcRepository.save(u);
                })
                .flatMap(user -> reactiveRedisTemplate.unlink(getUserCacheKey(id)).then(Mono.just(user)));

    }

}
