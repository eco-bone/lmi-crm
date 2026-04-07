package com.lmi.crm.service.crud;

import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dto.ProspectRequestDTO;
import com.lmi.crm.dto.ProspectResponseDTO;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.mapper.ProspectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProspectServiceImpl implements ProspectService {

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private ProspectMapper prospectMapper;

    @Override
    public ProspectResponseDTO createProspect(ProspectRequestDTO request) {
        log.info("Creating new prospect: {}", request.getCompanyName());
        Prospect prospect = prospectMapper.toEntity(request);
        return prospectMapper.toDTO(prospectRepository.save(prospect));
    }

    @Override
    public ProspectResponseDTO getProspectById(Integer id) {
        Prospect prospect = prospectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prospect with ID {} not found", id);
                    return new EntityNotFoundException("Prospect not found with ID: " + id);
                });
        return prospectMapper.toDTO(prospect);
    }

    @Override
    public List<ProspectResponseDTO> getAllProspects() {
        return prospectRepository.findAll()
                .stream()
                .map(prospectMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProspectResponseDTO updateProspect(Integer id, ProspectRequestDTO request) {
        log.info("Updating prospect with ID {}", id);
        Prospect prospect = prospectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prospect with ID {} not found", id);
                    return new EntityNotFoundException("Prospect not found with ID: " + id);
                });
        prospectMapper.updateEntity(prospect, request);
        return prospectMapper.toDTO(prospectRepository.save(prospect));
    }

    @Override
    public void deleteProspect(Integer id) {
        log.info("Soft deleting prospect with ID {}", id);
        Prospect prospect = prospectRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prospect with ID {} not found", id);
                    return new EntityNotFoundException("Prospect not found with ID: " + id);
                });
        prospect.setDeletionStatus(true);
        prospectRepository.save(prospect);
    }
}
