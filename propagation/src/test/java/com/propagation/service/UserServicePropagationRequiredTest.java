package com.propagation.service;

import com.propagation.User.service.required.UserServicePropagationRequired;
import com.propagation.User.service.supports.UserServicePropagationSupports;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserServicePropagationRequiredTest {

    @Autowired
    UserServicePropagationRequired userServicePropagationRequired;

    @Autowired
    UserServicePropagationSupports userServicePropagationSupports;
[]

    @BeforeEach
    public void saveUser() {
        userServicePropagationRequired.saveUser();
    }

    @AfterEach
    public void deleteUser() {
        userServicePropagationRequired.deleteUser();
    }

    @Test
    public void Transactional_required() throws InterruptedException {
//        int threadCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(threadCount);

        userServicePropagationRequired.updateParent(1L);

    }


    @Test
    public void Transactional_Supports() throws InterruptedException {
//        int threadCount = 10;
//        ExecutorService executorService = Executors.newFixedThreadPool(32);
//        CountDownLatch latch = new CountDownLatch(threadCount);

        userServicePropagationSupports.updateParentNoTx(1L);

    }



}