package com.lmi.crm.auth.dto;

import com.lmi.crm.enums.OtpType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotNull
    private Integer userId;

    @NotBlank
    private String otp;

    @NotNull
    private OtpType type;
}
