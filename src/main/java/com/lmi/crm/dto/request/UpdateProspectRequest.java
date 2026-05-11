package com.lmi.crm.dto.request;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProspectRequest {

    @Size(min = 1)
    private String companyName;

    @Size(min = 1)
    private String city;

    @Size(min = 1)
    private String contactFirstName;

    @Size(min = 1)
    private String contactLastName;

    private String designation;

    @Email
    private String email;

    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    private String phone;

    private String referredBy;

    private ClassificationType classificationType;

    private ProspectStatus status;

    private Integer protectionPeriodMonths;

    private Integer newLicenseeId;

    private Integer newAssociateId;
}
