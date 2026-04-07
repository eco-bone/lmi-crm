package com.lmi.crm.service.crud;

import com.lmi.crm.dto.GroupRequestDTO;
import com.lmi.crm.dto.GroupResponseDTO;

import java.util.List;

public interface GroupService {
    GroupResponseDTO createGroup(GroupRequestDTO request);
    GroupResponseDTO getGroupById(Integer id);
    List<GroupResponseDTO> getAllGroups();
    GroupResponseDTO updateGroup(Integer id, GroupRequestDTO request);
    void deleteGroup(Integer id);
}
