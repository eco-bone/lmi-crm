package com.lmi.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LicenseeCityResponseDTO {
    private Integer id;
    private Integer licenseeId;
    private String city;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}
