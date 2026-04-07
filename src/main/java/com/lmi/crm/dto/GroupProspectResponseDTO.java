package com.lmi.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupProspectResponseDTO {
    private Integer id;
    private Integer groupId;
    private Integer prospectId;
    private LocalDateTime createdAt;
}
