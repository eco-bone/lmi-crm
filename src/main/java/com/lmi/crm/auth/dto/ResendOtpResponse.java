package com.lmi.crm.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResendOtpResponse {

    /** Present only when an OTP was actually sent, so the client can call /setup/otp. */
    private Integer userId;

    private String message;
}
