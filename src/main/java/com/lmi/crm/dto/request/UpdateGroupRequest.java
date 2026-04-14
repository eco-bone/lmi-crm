package com.lmi.crm.dto.request;

import com.lmi.crm.enums.DeliveryType;
import com.lmi.crm.enums.GroupProgramType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateGroupRequest {

    @Min(1)
    @Max(100)
    private Integer groupSize;

    private GroupProgramType groupType;

    private DeliveryType deliveryType;

    private LocalDate startDate;

    private LocalDate ppmTfeDateSent;

    private Integer facilitatorId;

    private Integer licenseeId;

    private List<Integer> addProspectIds;

    private List<Integer> removeProspectIds;
}
