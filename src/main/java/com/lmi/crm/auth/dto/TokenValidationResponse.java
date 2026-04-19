package com.lmi.crm.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {

    private Boolean valid;
    private Integer userId;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private String message;
}
