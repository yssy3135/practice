package com.example.transactionreadonly.MemberTests;

import com.example.transactionreadonly.Member.domain.Member;
import com.example.transactionreadonly.Member.service.MemberService;
import com.example.transactionreadonly.Member.service.ReadOnlyMemberService;
import com.example.transactionreadonly.Member.service.dto.MemberRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.JpaSystemException;

import java.util.Optional;

@SpringBootTest
@Slf4j
public class MemberTest {

    @Autowired
    MemberService memberService;

    @Autowired
    ReadOnlyMemberService readOnlyMemberService;

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
     * flush는 나갈것이다 하지만 snapshot을 만들지 않기때문에 변경감지를 못한다.
     * 따라서 변경된 것이 없다고 판단할 것 그렇다고 save를 호출하면 exception이 발생한다.
     * 데이터를 변경하거나 추가할 수 없다!
     */
    @Test
    public void read_only_일경우_jpql_쿼리시_flush_안나간다() {
        Optional<Member> memberOptional = memberService.flushTest("tester","updated");

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

        Optional<Member> foundMemberOptional = memberService.findMember("updated");

        Assertions.assertFalse(foundMemberOptional.isPresent());
    }

    /**
     * readOnly 옵션을 true로 설정했을 경우
     * save문을 실행했을경우 트랜잭션이 끝나는 시점에 Exception이 발생한다.
     */
    @Test
    public void read_only_일경우_insert_불가() {

        Assertions.assertThrows(JpaSystemException.class,() -> {memberService.findAndInsert(MemberRequest.builder().name("insert").build());});
    }

    /**
     * 쿼리 자체가 나가지 않음 실행안됨 더티 체킹할 대상이 없다.
     * findById 해서 영속화를 시키고 변경을한 후 saveAndFlush를 통해 DB에 반영시키려 하지만 쿼리 나가지 않음
     * 다시 findById를 통해 조회하면 1차캐시에서 조회되기 때문에 값이 나옴 하지만 DB에 반영은 되지 않음.
     * 따라서 findByName jpql 쿼리를 통해 조회할 경우 값이 조회되지 않는다.
     */
    @Test
    public void read_only_일경우_수정_불가_쿼리X_exception_나오지_않음() {
        Member member = memberService.saveMember(
                MemberRequest.builder()
                        .name("tester")
                        .build());

        Optional<Member> updatedMemberOptional = memberService.readOnlyUpdateMember(member.getId(), "update");

        Assertions.assertFalse(updatedMemberOptional.isPresent());
    }


    @Test
    public void read_only_Transaction_propagation_Exception_발생하지_않음() {
        Member member = memberService.saveMember(
                MemberRequest.builder()
                        .name("tester")
                        .build());
        Member updatedMember = readOnlyMemberService.updateMemberRequiresNew(member.getId(), "updated");
        Assertions.assertEquals("updated", updatedMember.getName());

    }


    @Test
    public void read_only_Transaction_propagation_Exception_발생() {
        Member member = memberService.saveMember(
                MemberRequest.builder()
                        .name("tester")
                        .build());

        Assertions.assertThrows(JpaSystemException.class, () -> readOnlyMemberService.saveMember("newMember"));


    }





}
