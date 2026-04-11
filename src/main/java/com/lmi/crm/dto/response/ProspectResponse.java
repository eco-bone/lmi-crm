package com.lmi.crm.dto.response;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProtectionStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ProspectResponse {

    private Integer id;
    private String companyName;
    private String city;
    private String contactFirstName;
    private String contactLastName;
    private String designation;
    private String email;
    private String phone;
    private String referredBy;
    private ClassificationType classificationType;
    private ProspectProgramType programType;
    private ProspectType type;
    private ProtectionStatus protectionStatus;
    private ProspectStatus prospectStatus;
    private Integer protectionPeriodMonths;
    private LocalDate entryDate;
    private Integer associateId;
    private Integer licenseeId;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private String provisionReason;
}
