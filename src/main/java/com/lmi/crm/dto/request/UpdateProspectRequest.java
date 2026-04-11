package com.lmi.crm.dto.request;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProtectionStatus;
import jakarta.validation.constraints.Email;
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

    private String phone;

    private String referredBy;

    private ClassificationType classificationType;

    private ProtectionStatus protectionStatus;

    private Integer protectionPeriodMonths;

    private Integer newLicenseeId;
}
