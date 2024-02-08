package com.example.webflux.repository;

import com.example.webflux.domain.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserR2dbcRepository extends ReactiveCrudRepository<User, Long> {


}
