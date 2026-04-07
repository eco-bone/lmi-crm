package com.lmi.crm.mapper;

import com.lmi.crm.dto.ProspectLicenseeRequestDTO;
import com.lmi.crm.dto.ProspectLicenseeResponseDTO;
import com.lmi.crm.entity.ProspectLicensee;
import org.springframework.stereotype.Component;

@Component
public class ProspectLicenseeMapper {

    public ProspectLicensee toEntity(ProspectLicenseeRequestDTO dto) {
        ProspectLicensee prospectLicensee = new ProspectLicensee();
        prospectLicensee.setProspectId(dto.getProspectId());
        prospectLicensee.setLicenseeId(dto.getLicenseeId());
        prospectLicensee.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        return prospectLicensee;
    }

    public void updateEntity(ProspectLicensee prospectLicensee, ProspectLicenseeRequestDTO dto) {
        prospectLicensee.setProspectId(dto.getProspectId());
        prospectLicensee.setLicenseeId(dto.getLicenseeId());
        prospectLicensee.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
    }

    public ProspectLicenseeResponseDTO toDTO(ProspectLicensee prospectLicensee) {
        ProspectLicenseeResponseDTO dto = new ProspectLicenseeResponseDTO();
        dto.setId(prospectLicensee.getId());
        dto.setProspectId(prospectLicensee.getProspectId());
        dto.setLicenseeId(prospectLicensee.getLicenseeId());
        dto.setIsPrimary(prospectLicensee.getIsPrimary());
        dto.setAssignedAt(prospectLicensee.getAssignedAt());
        return dto;
    }
}
