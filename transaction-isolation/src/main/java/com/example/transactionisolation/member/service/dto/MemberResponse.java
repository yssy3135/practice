package com.example.transactionisolation.member.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Builder
@NoArgsConstructor
public class MemberResponse {

    Long id;

    String name;

}
