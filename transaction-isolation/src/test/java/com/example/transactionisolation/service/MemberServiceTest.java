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

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
@Slf4j
public class MemberServiceTest {

    @Autowired
    MemberService memberService;


    @BeforeEach
    public void before() throws InterruptedException {

    }

    @Test
    public void transaction_read_uncommitted() throws InterruptedException, ExecutionException {
        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> updated = CompletableFuture.runAsync(() -> {
            try {
                memberService.updateMember(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return memberService.findUserBy(savedMember.getId());
        });

        CompletableFuture.allOf(updated,foundMemberResult).join();

        assertEquals("updated", foundMemberResult.get().getName());

    }

    @Test
    @DisplayName("여러 스레드가 동시 호출시Dirty Read, Non-Repeatable Read, Phantom Read 현상이 모두 발생")
    public void transaction_read_uncommitted_completableFuture() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                memberService.updateMember(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
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
                memberService.updateMember(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            Member foundMember = memberService.findUserById_read_committed(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });

        assertEquals("before", foundMemberResult.get().getName());

        CompletableFuture.allOf(changeName,foundMemberResult);

    }


    @Test
    @DisplayName("Non-Repeatable Read 한 트랜잭션 내에서 반복 읽기를 수행하면 다른 트랜잭션의 커밋 여부에 따라 조회 결과가 달라지는 문제 발생")
    public void transaction_read_committed_completableFuture() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                memberService.updateMember(savedMember.getId(), "updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            Member member= null;
            try {
                member = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnResults("updated");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return member;

        });
        CompletableFuture.allOf(changeName,foundMemberResult);

        log.info("members {}", foundMemberResult.get());
        assertEquals("updated", foundMemberResult.get().getName());

    }


    //Phantom Read 한 트랜잭션에서 여러번 조회할 경우 중간에 다른 트랜잭션에 의해 추가된 레코드가 발견될 수 있다.
    @Test
    @DisplayName("REPEATABLE_READ 팬텀리드 발생 안함. mysql은 거의 발생 안함 gap Lock 때문")
    public void transaction_read_committed_not_phantom_read() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            try {
                // 처음 조회후 3초간 sleep 후 다시 조회
                members = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResult(savedMember.getId());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                // 중간에 레코드 추가
                memberService.saveMemberDelay(MemberRequest.builder().name("newMem").build());
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture.allOf(changeName,foundMemberResult);

        log.info("updatedMembers {}", changeName.get());
        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }


    @Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 조회 팬텀리드 발생")
    public void transaction_read_committed_phantom_read() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            try {
                // 처음 조회후 3초간 sleep 후 다시 조회
                members = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(savedMember.getId());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                // 중간에 레코드 추가
                memberService.saveMemberDelay(MemberRequest.builder().name("newMem").build());
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture.allOf(changeName,foundMemberResult);

        log.info("members {}", foundMemberResult.get());

        assertEquals(2, foundMemberResult.get().size());
    }


    @Test
    @DisplayName("serializable 아무런 문제 발생하지 않음")
    public void transaction_serializable() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            try {
                // 처음 조회후 3초간 sleep 후 다시 조회
                members = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLockSerializable(savedMember.getId());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            try {
                // 중간에 레코드 추가
                memberService.saveMemberDelay(MemberRequest.builder().name("newMem").build());
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        CompletableFuture.allOf(changeName,foundMemberResult);

        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
        assertEquals("before", foundMemberResult.get().get(0).getName());
    }






    public void sleep(int millis)  {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
