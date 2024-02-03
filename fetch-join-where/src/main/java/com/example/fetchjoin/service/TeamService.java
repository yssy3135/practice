package com.example.fetchjoin.service;

import com.example.fetchjoin.domain.Member;
import com.example.fetchjoin.domain.Team;
import com.example.fetchjoin.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    private final MemberService memberService;


    @Transactional
    public void saveAndFindTeam(String teamName, String memberName) {
        Optional<Team> teamOptional = teamRepository.findByTeamExceptMemberName(teamName, memberName);
        Team team = teamOptional.get();

        Member member = memberService.getMemberBy(memberName);
        team.addMember(member);
//        List<Member> members = team.getMembers();
//        members.get(0).updateName("훈이");

        Team saveTeam = teamRepository.saveAndFlush(team);
    }

    @Transactional
    public Team findTeamExceptMemberName(String teamName, String memberName) {
        Team team = teamRepository.findByTeamExceptMemberName(teamName, memberName)
                .orElseThrow(() -> new RuntimeException("not found"));

        team.changeName("나뭇잎마을");

        return team;
    }

}
