package com.lmi.crm.dto;

import lombok.Data;

@Data
public class ProspectLicenseeRequestDTO {
    private Integer prospectId;
    private Integer licenseeId;
    private Boolean isPrimary;
}
