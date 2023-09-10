package com.example.transactionreadonly.MemberTests;

import com.example.transactionreadonly.Member.domain.Member;
import com.example.transactionreadonly.Member.repository.MemberRepository;
import com.example.transactionreadonly.Member.service.MemberService;
import com.example.transactionreadonly.Member.service.dto.MemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.transaction.annotation.Transactional;

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


    /**
     * findMember 메소드에는 persist라는 Member를 save하고 find한다.
     * flush가 안나간다면 persist 라는 Member가 없어야 한다.
     * 아직 트랜잭션이 종료되지 않았기 때문에 DB에 반영이 안될을 것.
     * 하지만 readOnly = true여도 flush가 나갔기 때문에 조회시 결과값이 반환된다.
     */
    @Test
    public void read_only_일경우_jpql_쿼리시_flush_나간다() {
        Optional<Member> memberOptional = memberService.findMember("persist");

        Assertions.assertTrue(memberOptional.isPresent());

    }


    /**
     * 그럼 dirtyChecking은 일어날까?
     * 일어나지 않는다.
     * 왜냐하면 readOnly=true로 설정하면 애초에 조회시 snapshot을 만들지 않기때문에
     * 변경되었는지 감지 할 수 없다.
     */
    @Test
    public void read_only_일경우_jpql_쿼리시_flush_나감_그리고_dirty_checking_안일어난다() {
        Member member = memberService.updateMemberNotSave("updated");

        Assertions.assertEquals("tester",member.getName());

    }




}
