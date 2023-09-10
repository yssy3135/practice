package com.example.transactionisolation.Member.service.dto;

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
