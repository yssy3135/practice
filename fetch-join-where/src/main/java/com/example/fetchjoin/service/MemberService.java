package com.example.fetchjoin.service;

import com.example.fetchjoin.domain.Member;
import com.example.fetchjoin.repository.MemberRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final EntityManager em;



    public Member updateMember(Long id, String name) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        member.updateName(name);

        return memberRepository.save(member);
    }


    public Member getMemberBy(String name) {
        return memberRepository.findMemberByName(name).orElseThrow(() -> new RuntimeException("not found"));
    }


}
