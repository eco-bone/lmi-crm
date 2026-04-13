package com.lmi.crm.mapper;

import com.lmi.crm.dto.request.AddGroupRequest;
import com.lmi.crm.dto.response.GroupResponse;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.entity.Group;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class GroupMapper {

    public Group fromAddGroupRequest(AddGroupRequest request, Integer licenseeId,
                                     Integer facilitatorId, Integer createdBy) {
        return Group.builder()
                .licenseeId(licenseeId)
                .facilitatorId(facilitatorId)
                .groupSize(request.getGroupSize())
                .groupType(request.getGroupType())
                .deliveryType(request.getDeliveryType())
                .startDate(request.getStartDate())
                .ppmTfeDateSent(request.getPpmTfeDateSent())
                .createdBy(createdBy)
                .deletionStatus(false)
                .build();
    }

    public GroupResponse toResponse(Group group, String licenseeName, String facilitatorName,
                                    List<ProspectResponse> prospects) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setLicenseeId(group.getLicenseeId());
        response.setLicenseeName(licenseeName);
        response.setFacilitatorId(group.getFacilitatorId());
        response.setFacilitatorName(facilitatorName);
        response.setGroupSize(group.getGroupSize());
        response.setGroupType(group.getGroupType());
        response.setDeliveryType(group.getDeliveryType());
        response.setStartDate(group.getStartDate());
        response.setPpmTfeDateSent(group.getPpmTfeDateSent());
        response.setCreatedBy(group.getCreatedBy());
        response.setCreatedAt(group.getCreatedAt());
        response.setProspects(prospects);
        return response;
    }
}
