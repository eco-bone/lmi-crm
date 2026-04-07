package com.lmi.crm.mapper;

import com.lmi.crm.dto.GroupProspectRequestDTO;
import com.lmi.crm.dto.GroupProspectResponseDTO;
import com.lmi.crm.entity.GroupProspect;
import org.springframework.stereotype.Component;

@Component
public class GroupProspectMapper {

    public GroupProspect toEntity(GroupProspectRequestDTO dto) {
        GroupProspect groupProspect = new GroupProspect();
        groupProspect.setGroupId(dto.getGroupId());
        groupProspect.setProspectId(dto.getProspectId());
        return groupProspect;
    }

    public void updateEntity(GroupProspect groupProspect, GroupProspectRequestDTO dto) {
        groupProspect.setGroupId(dto.getGroupId());
        groupProspect.setProspectId(dto.getProspectId());
    }

    public GroupProspectResponseDTO toDTO(GroupProspect groupProspect) {
        GroupProspectResponseDTO dto = new GroupProspectResponseDTO();
        dto.setId(groupProspect.getId());
        dto.setGroupId(groupProspect.getGroupId());
        dto.setProspectId(groupProspect.getProspectId());
        dto.setCreatedAt(groupProspect.getCreatedAt());
        return dto;
    }
}
