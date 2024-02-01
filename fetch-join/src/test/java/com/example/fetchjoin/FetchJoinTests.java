package com.example.fetchjoin;

import com.example.fetchjoin.domain.Member;
import com.example.fetchjoin.domain.Team;
import com.example.fetchjoin.repository.MemberRepository;
import com.example.fetchjoin.repository.TeamRepository;
import com.example.fetchjoin.service.TeamService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class FetchJoinTests {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TeamRepository teamRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    TeamService teamService;



    @BeforeEach
    void before() {
        Member member1 = Member.builder().name("철수").build();
        Member member2 = Member.builder().name("영희").build();
        Member member3 = Member.builder().name("짱구").build();
        Member member4 = Member.builder().name("맹구").build();

        List<Member> members = List.of(member1, member2, member3, member4);
//        List<Member> members = memberRepository.saveAll(List.of(member1, member2, member3, member4));

        Team team = Team.builder().name("떡잎마을").build();
        team.setMembers(members);

        teamRepository.save(team);

        System.out.println("================================비포 ================================================");
    }

    @Test
    @Rollback(value = false)
    public void fetchJoinWhere() {
        Optional<Team> teamOptional = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희");

        assertThat(teamOptional.isPresent()).isTrue();
        assertThat(teamOptional.get()).isNotNull();
//        assertThat(teamOptional.get().getMembers().size()).isEqualTo(3);
        Team team = teamOptional.get();

//        entityManager.detach(team);

//        Member member = memberRepository.findMemberByName("영희").orElseThrow(() -> new RuntimeException("not found"));
//        team.addMember(member);
//        entityManager.detach(member);
        Team saveTeam = teamRepository.save(team);

//        assertThat(saveTeam.getMembers().size()).isEqualTo(4);
    }

    @Test
    @Rollback(value = false)
    public void teamDeleteTest() {
        Optional<Team> teamOptional = teamRepository.findTeamByTeamName("떡잎마을");
        assertThat(teamOptional.isPresent()).isTrue();
        Team team = teamOptional.get();


        List<Member> members = team.getMembers().subList(0, 1);
        team.setMembers(members);
        Team saveTeam = teamRepository.save(team);

        assertThat(saveTeam.getMembers().size()).isEqualTo(1);

    }

    @Test
    @Rollback(value = false)
    public void teamServiceTest() {
        teamService.saveAndFindTeam("떡잎마을", "영희");

    }
}
