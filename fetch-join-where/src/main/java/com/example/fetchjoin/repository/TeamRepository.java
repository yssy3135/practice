package com.example.fetchjoin.repository;

import com.example.fetchjoin.domain.Member;
import com.example.fetchjoin.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT t " +
            "FROM Team t JOIN FETCH t.members m " +
            "WHERE t.name = :teamName AND m.name != :memberName")
    Optional<Team> findByTeamExceptMemberName(String teamName, String memberName);

    @Query("SELECT t " +
            "FROM Team t JOIN FETCH t.members m " +
            "WHERE t.name = :teamName ")
    Optional<Team> findTeamByTeamName(String teamName);

}
