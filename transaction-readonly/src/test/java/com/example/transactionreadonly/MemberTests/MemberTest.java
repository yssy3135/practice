package com.example.transactionreadonly.MemberTests;

import com.example.transactionreadonly.Member.domain.Member;
import com.example.transactionreadonly.Member.service.MemberService;
import com.example.transactionreadonly.Member.service.dto.MemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister;

import java.util.Optional;

@SpringBootTest
@Slf4j
public class MemberTest {

    @Autowired
    MemberService memberService;


    @BeforeEach
    public void before() {
        memberService.saveMember(
                MemberRequest.builder()
                    .name("tester")
                .build());
    }


    @Test
    public void read_only_일경우_jpql_쿼리시_flush_나간다() {
        Optional<Member> memberOptional = memberService.findMember("persist");


        Assertions.assertTrue(memberOptional.isPresent());

    }




}
