package com.lmi.crm.dto.request;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectProgramType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @Email
    private String email;

    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    private String phone;

    @NotBlank
    private String referredBy;

    @NotNull
    private ClassificationType classificationType;

    @NotNull
    private ProspectProgramType programType;

    private Integer associateId;
}
