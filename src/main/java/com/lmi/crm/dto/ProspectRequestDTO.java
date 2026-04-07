package com.lmi.crm.dto;

import com.lmi.crm.enums.ClassificationType;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProtectionStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProspectRequestDTO {
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
    private Integer associateId;
    private LocalDate firstMeetingDate;
    private LocalDate lastMeetingDate;
    private LocalDate entryDate;
    private ProtectionStatus protectionStatus;
    private Integer protectionPeriodMonths;
}
