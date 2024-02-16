package com.example.webflux.controller;

import com.example.webflux.domain.User;
import com.example.webflux.dto.request.UserCreateRequest;
import com.example.webflux.dto.response.UserResponse;
import com.example.webflux.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(UserController.class)
@AutoConfigureWebTestClient
class UserControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserService userService;

    @Test
    void createUser() {

        Mockito.when(userService.create(Mockito.anyString(), Mockito.anyString())).thenReturn(
                Mono.just(User.builder().id(1L).name("greg").email("greg@test.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build())
        );

        webTestClient.post().uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UserCreateRequest("greg", "greg@test.com"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(UserResponse.class)
                .value(res -> {
                    assertEquals("greg", res.getName());
                    assertEquals("greg@test.com", res.getEmail());
                });

    }

    @Test
    void findAllUser() {
        Mockito.when(userService.findAll()).thenReturn(
                Flux.just(
                        User.builder().id(1L).name("greg1").email("greg@test.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        User.builder().id(2L).name("greg2").email("greg@test.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build(),
                        User.builder().id(3L).name("greg3").email("greg@test.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()
                )
        );


        webTestClient.get().uri("/users")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(UserResponse.class)
                .hasSize(3);


    }

    @Test
    void findUser() {
        Mockito.when(userService.findById(Mockito.anyLong())).thenReturn(
                Mono.just(User.builder().id(1L).name("greg").email("greg@test.com").createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build()));

        webTestClient.get().uri("/users/1")
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(UserResponse.class)
                .value(res -> {
                    assertEquals("greg", res.getName());
                    assertEquals("greg@test.com", res.getEmail());
                });

    }

    @Test
    void findNotFoundUser() {
        Mockito.when(userService.findById(Mockito.anyLong())).thenReturn(Mono.empty());

        webTestClient.get().uri("/users/1")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void deleteById() {
        Mockito.when(userService.deleteById(Mockito.anyLong()));

        webTestClient.delete().uri("/users/1")
                .exchange()
                .expectStatus().isNoContent();

    }

    @Test
    void updateUser() {
        Mockito.when(userService.update(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())).thenReturn(
                Mono.just(User.builder().id(1L).name("greg1").email("greg1@test.com").build())
        );

        webTestClient.put().uri("/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UserCreateRequest("greg1", "greg1@test.com"))
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(UserResponse.class)
                .value(res -> {
                    assertEquals("greg1", res.getName());
                    assertEquals("greg1@test.com", res.getEmail());
                });

    }

    @Test
    void updateUserNotFound() {
        Mockito.when(userService.update(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())).thenReturn(
                Mono.empty()
        );

        webTestClient.put().uri("/users/1")
                .exchange()
                .expectStatus().is4xxClientError();

    }
}