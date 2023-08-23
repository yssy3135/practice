package com.example.transactionisolation.User.service;

import com.example.transactionisolation.User.domain.Member;
import com.example.transactionisolation.User.repository.MemberRepository;
import com.example.transactionisolation.User.service.dto.MemberRequest;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    private final EntityManager entityManager;



    public Member saveMember(MemberRequest memberRequest)  {
        Member memberRequestEntity = memberRequest.toEntity();
        return memberRepository.save(memberRequestEntity);
    }

    @Transactional
    public Member updateDelay1sec(Long id, String name) throws InterruptedException {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        member.updateName("temp");
        memberRepository.flush();

        Thread.sleep(1000);

        member.updateName(name);
        memberRepository.flush();

        return memberRepository.save(member);
    }


    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Member findUserBy(Long id)  {
        return memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserById_read_committed(Long id) {

        return memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));

    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserByName_read_committed(String name) throws InterruptedException {
        Optional<Member> foundMember = memberRepository.findMemberByName(name);

        if(foundMember.isPresent()){
            log.info("foundMember {}", foundMember);
        }

        Thread.sleep(1000);

        Optional<Member> afterSleepFoundUser = memberRepository.findMemberByName(name);
        log.info("afterSleepFoundUser {}", afterSleepFoundUser);

        return afterSleepFoundUser.get();
    }

}
