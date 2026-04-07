package com.lmi.crm.service.crud;

import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dto.ProspectLicenseeRequestDTO;
import com.lmi.crm.dto.ProspectLicenseeResponseDTO;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.mapper.ProspectLicenseeMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProspectLicenseeServiceImpl implements ProspectLicenseeService {

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private ProspectLicenseeMapper prospectLicenseeMapper;

    @Override
    public ProspectLicenseeResponseDTO createProspectLicensee(ProspectLicenseeRequestDTO request) {
        log.info("Assigning prospect ID {} to licensee ID {}", request.getProspectId(), request.getLicenseeId());
        ProspectLicensee prospectLicensee = prospectLicenseeMapper.toEntity(request);
        return prospectLicenseeMapper.toDTO(prospectLicenseeRepository.save(prospectLicensee));
    }

    @Override
    public ProspectLicenseeResponseDTO getProspectLicenseeById(Integer id) {
        ProspectLicensee prospectLicensee = prospectLicenseeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ProspectLicensee with ID {} not found", id);
                    return new EntityNotFoundException("ProspectLicensee not found with ID: " + id);
                });
        return prospectLicenseeMapper.toDTO(prospectLicensee);
    }

    @Override
    public List<ProspectLicenseeResponseDTO> getAllProspectLicensees() {
        return prospectLicenseeRepository.findAll()
                .stream()
                .map(prospectLicenseeMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProspectLicenseeResponseDTO updateProspectLicensee(Integer id, ProspectLicenseeRequestDTO request) {
        log.info("Updating prospect licensee with ID {}", id);
        ProspectLicensee prospectLicensee = prospectLicenseeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ProspectLicensee with ID {} not found", id);
                    return new EntityNotFoundException("ProspectLicensee not found with ID: " + id);
                });
        prospectLicenseeMapper.updateEntity(prospectLicensee, request);
        return prospectLicenseeMapper.toDTO(prospectLicenseeRepository.save(prospectLicensee));
    }

    @Override
    public void deleteProspectLicensee(Integer id) {
        log.info("Deleting prospect licensee with ID {}", id);
        ProspectLicensee prospectLicensee = prospectLicenseeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("ProspectLicensee with ID {} not found", id);
                    return new EntityNotFoundException("ProspectLicensee not found with ID: " + id);
                });
        prospectLicenseeRepository.delete(prospectLicensee);
    }
}
