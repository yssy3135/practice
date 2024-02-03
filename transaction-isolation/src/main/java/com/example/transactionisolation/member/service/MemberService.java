package com.example.transactionisolation.member.service;

import com.example.transactionisolation.member.domain.Member;
import com.example.transactionisolation.member.repository.MemberRepository;
import com.example.transactionisolation.member.service.dto.MemberRequest;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final EntityManager em;


    @Transactional
    public Member updateMember(Long id, String name) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));

        member.updateName(name);
        memberRepository.flush();
        sleep(3000);

        log.info("*****update Member close*****");
        return memberRepository.save(member);
    }


    @Transactional
    public Member immediateUpdateMember(Long id, String name) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        member.updateName(name);

        log.info("*****update Member close*****");
        return memberRepository.saveAndFlush(member);
    }


    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Member findMemberBy(Long id)  {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        log.info("*****found Member : {}*****", member);
        return member;
    }




    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findMemberById_read_committed(Long id) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        log.info("*****found Member : {}*****", member);
        return member;
    }



    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Member findMemberRepeatable(String name) {

        return repeatableFind(name);

    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserByIdTwiceStopOnceInTheMiddleReturnResults(String name)  {

        return repeatableFind(name);

    }

    private Member repeatableFind(String name) {
        Optional<Member> member = memberRepository.findMemberByName(name);

        log.info("*****첫번쨰 Member : {}*****", member);

        sleep(3000);

        Optional<Member> secondMember = memberRepository.findMemberByName(name);
        log.info("*****두번쨰 Member : {}*****", secondMember);
        return secondMember.orElse(null);
    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserByName_read_committed(Long id) {
        Optional<Member> foundMember = memberRepository.findById(id);

        if(foundMember.isPresent()){
            log.info("foundMember {}", foundMember);
        }

        sleep(1000);

        Optional<Member> afterSleepFoundUser = memberRepository.findById(id);
        log.info("afterSleepFoundUser {}", afterSleepFoundUser);

        return afterSleepFoundUser.get();
    }

    /**
     * 일반적 MySQL의 REPEATABLE_READ 에서는 PhantomRead는 발생하지 않음 -> Gap Lock 에 의해
     * SELECT FOR UPDATE로 조회를 했다면, 언두 로그가 아닌 테이블로부터 레코드를 조회하므로 Phantom Read가 발생한다.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResult(Long id) {
        List<Member> members = memberRepository.findMembersByIdAfter(id);
        log.info("*****첫번째 Members : {}*****", members);
        sleep(3000);

        List<Member> secondMembers = memberRepository.findMembersByIdAfter(id);
        log.info("*****두번째 Members : {}*****", secondMembers);
        return secondMembers;
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> findMemberByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(Long id) {
        memberRepository.findMembersByIdAfter(id);

        sleep(1000);

        return memberRepository.findMembersByIdAfterForUpdate(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> selectForUpdateAfterSelect(Long id) {
        memberRepository.findMembersByIdAfterForUpdate(id);
        sleep(1000);

        return memberRepository.findMembersByIdAfter(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> selectForUpdateAfterSelectForUpdate(Long id)  {
        memberRepository.findMembersByIdAfterForUpdate(id);
        sleep(1000);

        return memberRepository.findMembersByIdAfterForUpdate(id);
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> selectAfterSelect(Long id) {
        memberRepository.findMembersByIdAfter(id);
        sleep(1000);

        return memberRepository.findMembersByIdAfter(id);
    }



    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Member> selectForUpdateAfterSelectForUpdateSerializable(Long id) {
        List<Member> members = memberRepository.findMembersByIdAfter(id);
        log.info("*****첫번째 Members : {}*****", members);
        sleep(3000);

        List<Member> secondMembers = memberRepository.findMembersByIdAfterForUpdate(id);
        log.info("*****두번째 Members : {}*****", secondMembers);
        return secondMembers;
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

}
