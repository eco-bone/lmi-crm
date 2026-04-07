package com.lmi.crm.dto;

import com.lmi.crm.enums.DeliveryType;
import com.lmi.crm.enums.GroupProgramType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GroupResponseDTO {
    private Integer id;
    private Integer licenseeId;
    private Integer facilitatorId;
    private Integer groupSize;
    private GroupProgramType groupType;
    private DeliveryType deliveryType;
    private LocalDate startDate;
    private LocalDate ppmTfeDateSent;
    private Integer createdBy;
    private Boolean deletionStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
