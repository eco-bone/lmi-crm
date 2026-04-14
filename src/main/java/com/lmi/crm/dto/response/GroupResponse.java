package com.lmi.crm.dto.response;

import com.lmi.crm.enums.DeliveryType;
import com.lmi.crm.enums.GroupProgramType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupResponse {

    private Integer id;
    private Integer licenseeId;
    private String licenseeName;
    private Integer facilitatorId;
    private String facilitatorName;
    private Integer groupSize;
    private GroupProgramType groupType;
    private DeliveryType deliveryType;
    private LocalDate startDate;
    private LocalDate ppmTfeDateSent;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private List<ProspectResponse> prospects;
}
