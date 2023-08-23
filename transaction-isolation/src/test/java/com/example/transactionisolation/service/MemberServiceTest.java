package com.example.transactionisolation.service;

import com.example.transactionisolation.User.domain.Member;
import com.example.transactionisolation.User.service.MemberService;
import com.example.transactionisolation.User.service.dto.MemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Slf4j
public class MemberServiceTest {


    @Autowired
    MemberService memberService;


    @BeforeEach
    public void before() throws InterruptedException {

    }


    @Test
    public void transaction_read_uncommitted() throws InterruptedException {

        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());


        executorService.submit(() -> {
            try {
                memberService.updateDelay1sec(savedMember.getId(),"updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                Thread.sleep(1000);
                Member foundMember = memberService.findUserBy(savedMember.getId());
                log.info("found member : {}", foundMember.toString());

                assertEquals("temp", foundMember.getName());
            } catch (RuntimeException e) {
                log.info("not found: {}", e.toString());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    @DisplayName("여러 스레드가 동시 호출시Dirty Read 발생")
    public void transaction_read_uncommitted_completableFuture() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                memberService.updateDelay1sec(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Member foundMember = memberService.findUserBy(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });

        assertEquals("temp", foundMemberResult.get().getName());

        CompletableFuture.allOf(changeName,foundMemberResult);

    }


    @Test
    @DisplayName("여러 스레드가 동시 호출시Dirty Read 발생 안함")
    public void transaction_read_committed_not_dirty_read() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                memberService.updateDelay1sec(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Member foundMember = memberService.findUserById_read_committed(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });

        assertEquals("before", foundMemberResult.get().getName());

        CompletableFuture.allOf(changeName,foundMemberResult);

    }


    @Test
    @DisplayName(" Non-Repeatable Read는 한 트랜잭션 내에서 반복 읽기를 수행하면 다른 트랜잭션의 커밋 여부에 따라 조회 결과가 달라지는 문제 발생")
    public void transaction_read_committed_completableFuture() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                memberService.updateDelay1sec(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Member foundMember = null;
            try {
                foundMember = memberService.findUserByName_read_committed("updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });

        assertEquals("updated", foundMemberResult.get().getName());

        CompletableFuture.allOf(changeName,foundMemberResult);

    }

}
