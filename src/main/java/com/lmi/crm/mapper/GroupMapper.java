package com.lmi.crm.mapper;

import com.lmi.crm.dto.GroupRequestDTO;
import com.lmi.crm.dto.GroupResponseDTO;
import com.lmi.crm.entity.Group;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public Group toEntity(GroupRequestDTO dto) {
        Group group = new Group();
        group.setLicenseeId(dto.getLicenseeId());
        group.setFacilitatorId(dto.getFacilitatorId());
        group.setGroupSize(dto.getGroupSize());
        group.setGroupType(dto.getGroupType());
        group.setDeliveryType(dto.getDeliveryType());
        group.setStartDate(dto.getStartDate());
        group.setPpmTfeDateSent(dto.getPpmTfeDateSent());
        group.setDeletionStatus(false);
        return group;
    }

    public void updateEntity(Group group, GroupRequestDTO dto) {
        group.setLicenseeId(dto.getLicenseeId());
        group.setFacilitatorId(dto.getFacilitatorId());
        group.setGroupSize(dto.getGroupSize());
        group.setGroupType(dto.getGroupType());
        group.setDeliveryType(dto.getDeliveryType());
        group.setStartDate(dto.getStartDate());
        group.setPpmTfeDateSent(dto.getPpmTfeDateSent());
    }

    public GroupResponseDTO toDTO(Group group) {
        GroupResponseDTO dto = new GroupResponseDTO();
        dto.setId(group.getId());
        dto.setLicenseeId(group.getLicenseeId());
        dto.setFacilitatorId(group.getFacilitatorId());
        dto.setGroupSize(group.getGroupSize());
        dto.setGroupType(group.getGroupType());
        dto.setDeliveryType(group.getDeliveryType());
        dto.setStartDate(group.getStartDate());
        dto.setPpmTfeDateSent(group.getPpmTfeDateSent());
        dto.setCreatedBy(group.getCreatedBy());
        dto.setDeletionStatus(group.getDeletionStatus());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        return dto;
    }
}
