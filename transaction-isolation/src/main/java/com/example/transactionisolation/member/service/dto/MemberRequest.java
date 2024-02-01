package com.example.transactionisolation.member.service.dto;

import com.example.transactionisolation.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class MemberRequest {


    private String name;


    public Member toEntity() {
        return Member.builder()
                .name(this.name)
                .build();
    }
}
