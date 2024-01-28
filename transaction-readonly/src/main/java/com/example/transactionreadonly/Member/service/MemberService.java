package com.example.transactionreadonly.Member.service;

import com.example.transactionreadonly.Member.domain.Member;
import com.example.transactionreadonly.Member.repository.MemberRepository;
import com.example.transactionreadonly.Member.service.dto.MemberRequest;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@AllArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final EntityManager em;

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Member findMemberBy(Long id)  {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        log.info("*****found Member : {}*****", member);
        return member;
    }

    public Member findMemberBy(String name)  {
        Member member = memberRepository.findMemberByName(name).orElseThrow(() -> new RuntimeException("Not found user"));
        log.info("*****found Member : {}*****", member);
        return member;
    }



    private Member repeatableFind(String name) {
        Optional<Member> member = memberRepository.findMemberByName(name);

        log.info("*****첫번쨰 Member : {}*****", member);

        sleep(3000);

        Optional<Member> secondMember = memberRepository.findMemberByName(name);
        log.info("*****두번쨰 Member : {}*****", secondMember);
        return secondMember.orElse(null);
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Member saveMember(MemberRequest memberRequest)  {
        Member memberRequestEntity = memberRequest.toEntity();

        return memberRepository.saveAndFlush(memberRequestEntity);
    }


    public void sleep(int millis)  {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Member updateMemberRequiresNew(Long id, String name) {
        Member member = findMemberBy(id);
        member.updateName(name);
        return member;
    }

    @Transactional(readOnly = true)
    public Optional<Member> flushTest(String name, String updateName) {
        Member member = memberRepository.findMemberByName(name).orElseThrow(() -> new RuntimeException("not found member"));
        member.updateName(updateName);

        memberRepository.flush();
        return memberRepository.findMemberByName(name);

    }

    @Transactional(readOnly = true)
    public Member updateMemberNotSave(String name) {
        Member member = findMemberBy("tester");
        member.updateName(name);
        return member;
    }




    public Optional<Member> findMember(String name) {
        return memberRepository.findMemberByName(name);
    }

    public Member findAndInsert(MemberRequest memberRequest) {
        Member member = memberRequest.toEntity();
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Optional<Member> readOnlyUpdateMember(Long id, String name) {
        Member member = findMemberBy(id);
        member.updateName(name);
        memberRepository.save(member);

        return memberRepository.findMemberByName(name);
    }
}
