package com.example.transactionreadonly.Member.service;


import com.example.transactionreadonly.Member.domain.Member;
import com.example.transactionreadonly.Member.service.dto.MemberRequest;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class ReadOnlyMemberService {

    private final MemberService memberService;

    private final EntityManager em;


    @Transactional(readOnly = true)
    public Member updateMemberRequiresNew(Long id, String name) {

        return memberService.updateMemberRequiresNew(id,name);
    }


    @Transactional(readOnly = true)
    public Member saveMember(String name) {

        return memberService.saveMember(MemberRequest.builder().name(name).build());
    }

}
