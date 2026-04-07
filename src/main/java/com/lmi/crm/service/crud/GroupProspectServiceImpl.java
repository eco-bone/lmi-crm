package com.lmi.crm.service.crud;

import com.lmi.crm.dao.GroupProspectRepository;
import com.lmi.crm.dto.GroupProspectRequestDTO;
import com.lmi.crm.dto.GroupProspectResponseDTO;
import com.lmi.crm.entity.GroupProspect;
import com.lmi.crm.mapper.GroupProspectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GroupProspectServiceImpl implements GroupProspectService {

    @Autowired
    private GroupProspectRepository groupProspectRepository;

    @Autowired
    private GroupProspectMapper groupProspectMapper;

    @Override
    public GroupProspectResponseDTO createGroupProspect(GroupProspectRequestDTO request) {
        log.info("Adding prospect ID {} to group ID {}", request.getProspectId(), request.getGroupId());
        GroupProspect groupProspect = groupProspectMapper.toEntity(request);
        return groupProspectMapper.toDTO(groupProspectRepository.save(groupProspect));
    }

    @Override
    public GroupProspectResponseDTO getGroupProspectById(Integer id) {
        GroupProspect groupProspect = groupProspectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("GroupProspect with ID {} not found", id);
                    return new EntityNotFoundException("GroupProspect not found with ID: " + id);
                });
        return groupProspectMapper.toDTO(groupProspect);
    }

    @Override
    public List<GroupProspectResponseDTO> getAllGroupProspects() {
        return groupProspectRepository.findAll()
                .stream()
                .map(groupProspectMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public GroupProspectResponseDTO updateGroupProspect(Integer id, GroupProspectRequestDTO request) {
        log.info("Updating group prospect with ID {}", id);
        GroupProspect groupProspect = groupProspectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("GroupProspect with ID {} not found", id);
                    return new EntityNotFoundException("GroupProspect not found with ID: " + id);
                });
        groupProspectMapper.updateEntity(groupProspect, request);
        return groupProspectMapper.toDTO(groupProspectRepository.save(groupProspect));
    }

    @Override
    public void deleteGroupProspect(Integer id) {
        log.info("Deleting group prospect with ID {}", id);
        GroupProspect groupProspect = groupProspectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("GroupProspect with ID {} not found", id);
                    return new EntityNotFoundException("GroupProspect not found with ID: " + id);
                });
        groupProspectRepository.delete(groupProspect);
    }
}
