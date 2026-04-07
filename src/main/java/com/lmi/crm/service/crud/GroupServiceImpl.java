package com.lmi.crm.service.crud;

import com.lmi.crm.dao.GroupRepository;
import com.lmi.crm.dto.GroupRequestDTO;
import com.lmi.crm.dto.GroupResponseDTO;
import com.lmi.crm.entity.Group;
import com.lmi.crm.mapper.GroupMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupServiceImpl implements GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMapper groupMapper;

    @Override
    public GroupResponseDTO createGroup(GroupRequestDTO request) {
        log.info("Creating new group for licensee ID {}", request.getLicenseeId());
        Group group = groupMapper.toEntity(request);
        return groupMapper.toDTO(groupRepository.save(group));
    }

    @Override
    public GroupResponseDTO getGroupById(Integer id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Group with ID {} not found", id);
                    return new EntityNotFoundException("Group not found with ID: " + id);
                });
        return groupMapper.toDTO(group);
    }

    @Override
    public List<GroupResponseDTO> getAllGroups() {
        return groupRepository.findAll()
                .stream()
                .map(groupMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GroupResponseDTO updateGroup(Integer id, GroupRequestDTO request) {
        log.info("Updating group with ID {}", id);
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Group with ID {} not found", id);
                    return new EntityNotFoundException("Group not found with ID: " + id);
                });
        groupMapper.updateEntity(group, request);
        return groupMapper.toDTO(groupRepository.save(group));
    }

    @Override
    public void deleteGroup(Integer id) {
        log.info("Soft deleting group with ID {}", id);
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Group with ID {} not found", id);
                    return new EntityNotFoundException("Group not found with ID: " + id);
                });
        group.setDeletionStatus(true);
        groupRepository.save(group);
    }
}
