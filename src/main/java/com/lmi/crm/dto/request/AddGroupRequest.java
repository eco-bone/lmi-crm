package com.lmi.crm.dto.request;

import com.lmi.crm.enums.DeliveryType;
import com.lmi.crm.enums.GroupProgramType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AddGroupRequest {

    @NotNull
    @Min(1)
    @Max(100)
    private Integer groupSize;

    @NotNull
    private GroupProgramType groupType;

    @NotNull
    private DeliveryType deliveryType;

    @NotNull
    private LocalDate startDate;

    private LocalDate ppmTfeDateSent;

    private Integer facilitatorId;

    @NotNull
    @NotEmpty
    private List<Integer> prospectIds;
}
