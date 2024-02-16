package com.example.webflux.repository;

import com.example.webflux.domain.User;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserR2dbcRepository extends ReactiveCrudRepository<User, Long> {

    Flux<Mono> findByName(String name);

    Flux<User> findByNameOrderByIdDesc(String name);

    @Modifying
    @Query("DELETE FROM users where name = :name")
    Mono<Void> deleteByName(String name);

}
