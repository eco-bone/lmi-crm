package com.lmi.crm.dto;

import com.lmi.crm.enums.DeliveryType;
import com.lmi.crm.enums.GroupProgramType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class GroupRequestDTO {
    private Integer licenseeId;
    private Integer facilitatorId;
    private Integer groupSize;
    private GroupProgramType groupType;
    private DeliveryType deliveryType;
    private LocalDate startDate;
    private LocalDate ppmTfeDateSent;
}
