package com.lmi.crm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupPasswordRequest {

    @NotBlank
    @Size(min = 8)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
