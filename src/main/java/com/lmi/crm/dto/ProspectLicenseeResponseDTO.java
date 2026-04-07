package com.lmi.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProspectLicenseeResponseDTO {
    private Integer id;
    private Integer prospectId;
    private Integer licenseeId;
    private Boolean isPrimary;
    private LocalDateTime assignedAt;
}
