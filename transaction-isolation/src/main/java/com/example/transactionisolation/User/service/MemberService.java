package com.example.transactionisolation.User.service;

import com.example.transactionisolation.User.domain.Member;
import com.example.transactionisolation.User.repository.MemberRepository;
import com.example.transactionisolation.User.service.dto.MemberRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;


    public Member saveMember(MemberRequest memberRequest)  {
        Member memberRequestEntity = memberRequest.toEntity();
        return memberRepository.save(memberRequestEntity);
    }

    @Transactional
    public Member updateMember(Long id, String name) throws InterruptedException {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));

        member.updateName(name);
        memberRepository.flush();
        Thread.sleep(3000);

        log.info("*****update Member close*****");
        return memberRepository.save(member);
    }


    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Member findUserBy(Long id)  {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        log.info("*****found Member : {}*****", member);
        return member;
    }




    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserById_read_committed(Long id) {

        return memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));

    }


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserByIdTwiceStopOnceInTheMiddleReturnResults(String name) throws InterruptedException {
        ArrayList<Member> memberList  = new ArrayList<>();

        memberRepository.findMemberByName("updated");

        Thread.sleep(3000);

        return memberRepository.findMemberByName("updated").orElseThrow(() -> new RuntimeException("Not found user"));

    }




    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserByName_read_committed(Long id) throws InterruptedException {
        Optional<Member> foundMember = memberRepository.findById(id);

        if(foundMember.isPresent()){
            log.info("foundMember {}", foundMember);
        }

        Thread.sleep(1000);

        Optional<Member> afterSleepFoundUser = memberRepository.findById(id);
        log.info("afterSleepFoundUser {}", afterSleepFoundUser);

        return afterSleepFoundUser.get();
    }

    /**
     * 일반적 MySQL의 REPEATABLE_READ 에서는 PhantomRead는 발생하지 않음 -> Gap Lock 에 의해
     * SELECT FOR UPDATE로 조회를 했다면, 언두 로그가 아닌 테이블로부터 레코드를 조회하므로 Phantom Read가 발생한다.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResult(Long id) throws InterruptedException {
        ArrayList<Member> memberList = new ArrayList<>();
        // 1. 조회
        memberList.addAll(memberRepository.findMembersByIdGreaterThanEqual(id));

        Thread.sleep(3000);

        // 3. 베타적 잠금을 건후 조회. ( SELECT … FOR UPDATE 구문 )
        return memberRepository.findMembersByIdAfterForUpdate(id);
    }


    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(Long id) throws InterruptedException {
        ArrayList<Member> memberList = new ArrayList<>();
        // 1. 조회
        memberList.addAll(memberRepository.findMembersByIdGreaterThanEqual(id));

        Thread.sleep(3000);

        // 3. 베타적 잠금을 건후 조회. ( SELECT … FOR UPDATE 구문 )
        return memberRepository.findMembersByIdAfterForUpdateUsingLock(id);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<Member> findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLockSerializable(Long id) throws InterruptedException {
        ArrayList<Member> memberList = new ArrayList<>();
        // 1. 조회
        memberList.addAll(memberRepository.findMembersByIdGreaterThanEqual(id));

        Thread.sleep(3000);

        // 3. 베타적 잠금을 건후 조회. ( SELECT … FOR UPDATE 구문 )
        return memberRepository.findMembersByIdAfterForUpdateUsingLock(id);
    }





    public Member saveMemberDelay(MemberRequest memberRequest) throws InterruptedException {
        Thread.sleep(3000);
        Member memberRequestEntity = memberRequest.toEntity();
        //2. 새로운 member 저장 후 commit
        return memberRepository.saveAndFlush(memberRequestEntity);
    }


}
