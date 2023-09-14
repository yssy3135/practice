package com.example.transactionisolation.service;

import com.example.transactionisolation.Member.domain.Member;
import com.example.transactionisolation.Member.service.MemberService;
import com.example.transactionisolation.Member.service.dto.MemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
public class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Test
    @DisplayName("uncommitted 수준 dirty read")
    public void shouldDirtyReadWhenIsolationLevelReadUncommitted() throws InterruptedException, ExecutionException {
        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> updated = CompletableFuture.runAsync(() -> {
            memberService.updateMember(savedMember.getId(), "updated");

        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return memberService.findMemberBy(savedMember.getId());
        });

        CompletableFuture.allOf(updated,foundMemberResult).join();

        assertEquals("updated", foundMemberResult.get().getName());

    }

    @Test
    @DisplayName("READ_UNCOMMITTED 여러 스레드가 동시 호출시Dirty Read, Non-Repeatable Read, Phantom Read 현상이 모두 발생")
    public void shouldDirtyReadAndPhantomReadWhenIsolationLevelReadUncommitted() throws InterruptedException, ExecutionException {
        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            memberService.updateMember(savedMember.getId(), "updated");

        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            Member foundMember = memberService.findMemberBy(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        assertEquals("updated", foundMemberResult.get().getName());
    }


    @Test
    @DisplayName("READ_COMMITTED 여러 스레드가 동시 호출시Dirty Read 발생 안함")
    public void shouldNotDirtyReadWhenIsolationLevelReadCommitted() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            memberService.updateMember(savedMember.getId(), "updated");
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            Member foundMember = memberService.findMemberById_read_committed(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });

        CompletableFuture.allOf(changeName,foundMemberResult).join();

        assertEquals("before", foundMemberResult.get().getName());
    }


    @Test
    @DisplayName("READ_COMMITTED Non-Repeatable Read 한 트랜잭션 내에서 반복 읽기를 수행하면 다른 트랜잭션의 커밋 여부에 따라 조회 결과가 달라지는 문제 발생")
    public void shouldNonRepeatableReadWhenIsolationLevelReadCommitted() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.immediateUpdateMember(savedMember.getId(), "updated");
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            Member member= null;
            member = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnResults("updated");

            return member;

        });

        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());
        assertEquals("updated", foundMemberResult.get().getName());

    }

    @Test
    @DisplayName("REPEATABLE_READ NON-REPEATABLE READ 현상 발생 안함.")
    public void shouldNotNonRepeatableReadWhenIsolationLevelRepeatableRead() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.immediateUpdateMember(savedMember.getId(), "updated");
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            Member member= null;
            member = memberService.findMemberRepeatable("updated");

            return member;

        });

        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());
        assertNull(foundMemberResult.get());

    }


    @Test
    @DisplayName("REPEATABLE_READ 팬텀리드 발생 안함. Select 이후 Select ")
    public void shouldNotPhantomReadWhenIsolationLevelRepeatableReadSelectAfterSelect() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            // 처음 조회후 3초간 sleep 후 다시 조회
            members = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResult(savedMember.getId());

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("updatedMembers {}", changeName.get());
        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }


    @Test
    @DisplayName("REPEATABLE_READ SELECT 이후 SELECT FOR UPDATE 조회 팬텀리드 발생")
    public void shouldPhantomReadWhenIsolationLevelRepeatableRead() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.findMemberByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(savedMember.getId());

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());

        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(2, foundMemberResult.get().size());
    }

    @Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 이후 SELECT_FOR_UPDATE 조회 팬텀리드X")
    public void shouldNotPhantomReadWhenIsolationLevelRepeatableReadSelectForUpdateAfterSelectForUpdated() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.selectForUpdateAfterSelectForUpdate(savedMember.getId());

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }


    @Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 이후 SELECT ")
    public void shouldNotPhantomReadWhenIsolationLevelRepeatableReadSelectForUpdateAfterSelect() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.selectForUpdateAfterSelect(savedMember.getId());
            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }


    @Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 조회 팬텀리드 발생")
    public void shouldPhantomReadWhenIsolationLevelRepeatableReadSelectForUpdate() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
        List<Member> members = null;
        members = memberService.findMemberByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(savedMember.getId());
            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(2, foundMemberResult.get().size());
    }


    @Test
    @DisplayName("serializable 아무런 문제 발생하지 않음")
    public void shouldNoProblemWhenIsolationLevelSerializable() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.selectForUpdateAfterSelectForUpdateSerializable(savedMember.getId());
            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
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
