package com.example.fetchjoin;

import com.example.fetchjoin.domain.Member;
import com.example.fetchjoin.domain.Team;
import com.example.fetchjoin.repository.MemberRepository;
import com.example.fetchjoin.repository.TeamRepository;
import com.example.fetchjoin.service.TeamService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
        memberRepository.saveAll(members);
        Team team = Team.builder().name("떡잎마을").build();
        team.setMembers(members);

        teamRepository.saveAndFlush(team);

        entityManager.clear();
        System.out.println("================================세팅================================================");
    }


    @Test
    @Transactional
    @DisplayName("Team을 변경하지 않고 그대로")
    public void saveTeam() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        assertThat(team.getMembers()).size().isEqualTo(3);

        Team saveTeam = teamRepository.saveAndFlush(team);
        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));
        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }

    @Test
    @Transactional
    @DisplayName("Team을 변경하고 저장했을 경우")
    public void changeTeam() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);

        team.changeName("나뭇잎마을");
        Team saveTeam = teamRepository.saveAndFlush(team);
        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("나뭇잎마을").orElseThrow(() -> new RuntimeException("not found"));
        assertThat(findTeam.getName()).isEqualTo("나뭇잎마을");
        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }


    @Test
    @Transactional
    @DisplayName("member - 영희을(DB 상 이미 team에 속해있는) 추가해서 save를 하면 update쿼리가 수행될까?")
    public void changeTeamAndAddMember() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);

        Member member = memberRepository.findMemberByName("영희").orElseThrow(() -> new RuntimeException("not found"));
        team.addMember(member);

        Team saveTeam = teamRepository.saveAndFlush(team);
        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));
        assertThat(findTeam.getName()).isEqualTo("떡잎마을");
        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }



    @Test
    @Transactional
    @DisplayName("Team에서 member를 하나 삭제하고 저장했을경우 DB상에 연관되어져있던 member는 어떻게될까?")
    public void removeMember() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);
        team.getMembers().remove(team.getMembers().stream().filter(member -> member.getName().equals("철수")).findAny().orElseThrow(() -> new RuntimeException("not found")));

        Team saveTeam = teamRepository.saveAndFlush(team);

        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));

        assertThat(findTeam.getMembers().size()).isEqualTo(3);
    }


    @Test
    @Transactional
    @DisplayName("Member를 2명만 새로운 리스트를 통해 전달받아 제거")
    public void removeMemberWhenNewList() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);
        team.setMembers(team.getMembers().subList(0,2));

        Team saveTeam = teamRepository.saveAndFlush(team);

        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));

        assertThat(findTeam.getMembers().size()).isEqualTo(2);
    }

    @Test
    @Transactional
    @DisplayName("DB상 원래 존재하는 Member - 영희를 Team - 떡잎마을에 다시 추가한다면?")
    public void addMember() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        assertThat(team.getMembers()).size().isEqualTo(3);
        Member member = memberRepository.findMemberByName("영희").orElseThrow(() -> new RuntimeException("not found"));
        team.getMembers().add(member);
        Team saveTeam = teamRepository.saveAndFlush(team);

        entityManager.clear();
        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을")
                .orElseThrow(() -> new RuntimeException("not found"));

        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }



}
