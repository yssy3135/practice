package com.example.transactionisolation.member.domain;

import jakarta.persistence.*;
import lombok.*;

@ToString
@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    public String updateName(String name) {
        return this.name = name;
    }

}
