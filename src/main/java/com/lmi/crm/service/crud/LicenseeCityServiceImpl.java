package com.lmi.crm.service.crud;

import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dto.LicenseeCityRequestDTO;
import com.lmi.crm.dto.LicenseeCityResponseDTO;
import com.lmi.crm.entity.LicenseeCity;
import com.lmi.crm.mapper.LicenseeCityMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LicenseeCityServiceImpl implements LicenseeCityService {

    @Autowired
    private LicenseeCityRepository licenseeCityRepository;

    @Autowired
    private LicenseeCityMapper licenseeCityMapper;

    @Override
    public LicenseeCityResponseDTO createLicenseeCity(LicenseeCityRequestDTO request) {
        log.info("Adding city {} for licensee ID {}", request.getCity(), request.getLicenseeId());
        LicenseeCity licenseeCity = licenseeCityMapper.toEntity(request);
        return licenseeCityMapper.toDTO(licenseeCityRepository.save(licenseeCity));
    }

    @Override
    public LicenseeCityResponseDTO getLicenseeCityById(Integer id) {
        LicenseeCity licenseeCity = licenseeCityRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("LicenseeCity with ID {} not found", id);
                    return new EntityNotFoundException("LicenseeCity not found with ID: " + id);
                });
        return licenseeCityMapper.toDTO(licenseeCity);
    }

    @Override
    public List<LicenseeCityResponseDTO> getAllLicenseeCities() {
        return licenseeCityRepository.findAll()
                .stream()
                .map(licenseeCityMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public LicenseeCityResponseDTO updateLicenseeCity(Integer id, LicenseeCityRequestDTO request) {
        log.info("Updating licensee city with ID {}", id);
        LicenseeCity licenseeCity = licenseeCityRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("LicenseeCity with ID {} not found", id);
                    return new EntityNotFoundException("LicenseeCity not found with ID: " + id);
                });
        licenseeCityMapper.updateEntity(licenseeCity, request);
        return licenseeCityMapper.toDTO(licenseeCityRepository.save(licenseeCity));
    }

    @Override
    public void deleteLicenseeCity(Integer id) {
        log.info("Deleting licensee city with ID {}", id);
        LicenseeCity licenseeCity = licenseeCityRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("LicenseeCity with ID {} not found", id);
                    return new EntityNotFoundException("LicenseeCity not found with ID: " + id);
                });
        licenseeCityRepository.delete(licenseeCity);
    }
}
