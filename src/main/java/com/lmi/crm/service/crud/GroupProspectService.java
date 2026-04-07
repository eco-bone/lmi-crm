package com.lmi.crm.service.crud;

import com.lmi.crm.dto.GroupProspectRequestDTO;
import com.lmi.crm.dto.GroupProspectResponseDTO;

import java.util.List;

public interface GroupProspectService {
    GroupProspectResponseDTO createGroupProspect(GroupProspectRequestDTO request);
    GroupProspectResponseDTO getGroupProspectById(Integer id);
    List<GroupProspectResponseDTO> getAllGroupProspects();
    GroupProspectResponseDTO updateGroupProspect(Integer id, GroupProspectRequestDTO request);
    void deleteGroupProspect(Integer id);
}
