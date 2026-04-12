package com.lmi.crm.dto.request;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectProgramType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddProspectRequest {

    @NotBlank
    private String companyName;

    @NotBlank
    private String city;

    @NotBlank
    private String contactFirstName;

    @NotBlank
    private String contactLastName;

    private String designation;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    @NotBlank
    private String referredBy;

    @NotNull
    private ClassificationType classificationType;

    @NotNull
    private ProspectProgramType programType;
}
