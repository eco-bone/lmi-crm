package com.lmi.crm.service.crud;

import com.lmi.crm.dto.LicenseeCityRequestDTO;
import com.lmi.crm.dto.LicenseeCityResponseDTO;

import java.util.List;

public interface LicenseeCityService {
    LicenseeCityResponseDTO createLicenseeCity(LicenseeCityRequestDTO request);
    LicenseeCityResponseDTO getLicenseeCityById(Integer id);
    List<LicenseeCityResponseDTO> getAllLicenseeCities();
    LicenseeCityResponseDTO updateLicenseeCity(Integer id, LicenseeCityRequestDTO request);
    void deleteLicenseeCity(Integer id);
}
