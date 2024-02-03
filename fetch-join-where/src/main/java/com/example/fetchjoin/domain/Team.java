package com.example.fetchjoin.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @OneToMany
    @JoinColumn(name = "memberId")
    List<Member> members = new ArrayList<>();

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public void replaceMembers(List<Member> members) {
        this.members.clear();
        this.members.addAll(members);
    }
    public void addMember(Member member) {
        this.members.add(member);
    }

    public void changeName(String name) {
        this.name = name;
    }
}
