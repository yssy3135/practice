package com.example.transactionisolation.User.repository;

import com.example.transactionisolation.User.domain.Member;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("select m from Member m where m.name = :name")
    Optional<Member> findMemberByName(String name);

    List<Member> findMembersByIdGreaterThanEqual(Long id);


    @Query("select m from Member m where m.id >= :id ")
    List<Member> findMembersByIdAfterForUpdate(Long id);

    @Query("select m from Member m where m.id >= :id ")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findMembersByIdAfterForUpdateUsingLock(Long id);

}
