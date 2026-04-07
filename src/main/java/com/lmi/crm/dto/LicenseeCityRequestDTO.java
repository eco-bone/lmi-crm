package com.lmi.crm.dto;

import lombok.Data;

@Data
public class LicenseeCityRequestDTO {
    private Integer licenseeId;
    private String city;
    private Boolean isPrimary;
}
