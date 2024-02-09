package com.example.webflux.service;

import com.example.webflux.domain.User;
import com.example.webflux.repository.UserR2dbcRepository;
import com.example.webflux.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService {

//    private final UserRepository userRepository;
    private final UserR2dbcRepository userR2dbcRepository;


    public Mono<User> create(String name, String email) {

        return userR2dbcRepository.save(User.builder().name(name).email(email).build());
    }

    public Flux<User> findAll() {
        return userR2dbcRepository.findAll();
    }

    public Mono<User> findById(Long id) {
        return userR2dbcRepository.findById(id);
    }

    public Mono<Void> deleteById(Long id) {
        return userR2dbcRepository.deleteById(id);
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
                });

    }

}
