package com.lmi.crm.mapper;

import com.lmi.crm.dto.LicenseeCityRequestDTO;
import com.lmi.crm.dto.LicenseeCityResponseDTO;
import com.lmi.crm.entity.LicenseeCity;
import org.springframework.stereotype.Component;

@Component
public class LicenseeCityMapper {

    public LicenseeCity toEntity(LicenseeCityRequestDTO dto) {
        LicenseeCity licenseeCity = new LicenseeCity();
        licenseeCity.setLicenseeId(dto.getLicenseeId());
        licenseeCity.setCity(dto.getCity());
        licenseeCity.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        return licenseeCity;
    }

    public void updateEntity(LicenseeCity licenseeCity, LicenseeCityRequestDTO dto) {
        licenseeCity.setLicenseeId(dto.getLicenseeId());
        licenseeCity.setCity(dto.getCity());
        licenseeCity.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
    }

    public LicenseeCityResponseDTO toDTO(LicenseeCity licenseeCity) {
        LicenseeCityResponseDTO dto = new LicenseeCityResponseDTO();
        dto.setId(licenseeCity.getId());
        dto.setLicenseeId(licenseeCity.getLicenseeId());
        dto.setCity(licenseeCity.getCity());
        dto.setIsPrimary(licenseeCity.getIsPrimary());
        dto.setCreatedAt(licenseeCity.getCreatedAt());
        return dto;
    }
}
