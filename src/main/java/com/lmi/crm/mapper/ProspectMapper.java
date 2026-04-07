package com.lmi.crm.mapper;

import com.lmi.crm.dto.ProspectRequestDTO;
import com.lmi.crm.dto.ProspectResponseDTO;
import com.lmi.crm.entity.Prospect;
import org.springframework.stereotype.Component;

@Component
public class ProspectMapper {

    public Prospect toEntity(ProspectRequestDTO dto) {
        Prospect prospect = new Prospect();
        prospect.setCompanyName(dto.getCompanyName());
        prospect.setCity(dto.getCity());
        prospect.setContactFirstName(dto.getContactFirstName());
        prospect.setContactLastName(dto.getContactLastName());
        prospect.setDesignation(dto.getDesignation());
        prospect.setEmail(dto.getEmail());
        prospect.setPhone(dto.getPhone());
        prospect.setReferredBy(dto.getReferredBy());
        prospect.setClassificationType(dto.getClassificationType());
        prospect.setProgramType(dto.getProgramType());
        prospect.setType(dto.getType());
        prospect.setAssociateId(dto.getAssociateId());
        prospect.setFirstMeetingDate(dto.getFirstMeetingDate());
        prospect.setLastMeetingDate(dto.getLastMeetingDate());
        prospect.setEntryDate(dto.getEntryDate());
        prospect.setProtectionStatus(dto.getProtectionStatus());
        prospect.setProtectionPeriodMonths(dto.getProtectionPeriodMonths());
        prospect.setDeletionStatus(false);
        return prospect;
    }

    public void updateEntity(Prospect prospect, ProspectRequestDTO dto) {
        prospect.setCompanyName(dto.getCompanyName());
        prospect.setCity(dto.getCity());
        prospect.setContactFirstName(dto.getContactFirstName());
        prospect.setContactLastName(dto.getContactLastName());
        prospect.setDesignation(dto.getDesignation());
        prospect.setEmail(dto.getEmail());
        prospect.setPhone(dto.getPhone());
        prospect.setReferredBy(dto.getReferredBy());
        prospect.setClassificationType(dto.getClassificationType());
        prospect.setProgramType(dto.getProgramType());
        prospect.setType(dto.getType());
        prospect.setAssociateId(dto.getAssociateId());
        prospect.setFirstMeetingDate(dto.getFirstMeetingDate());
        prospect.setLastMeetingDate(dto.getLastMeetingDate());
        prospect.setEntryDate(dto.getEntryDate());
        prospect.setProtectionStatus(dto.getProtectionStatus());
        prospect.setProtectionPeriodMonths(dto.getProtectionPeriodMonths());
    }

    public ProspectResponseDTO toDTO(Prospect prospect) {
        ProspectResponseDTO dto = new ProspectResponseDTO();
        dto.setId(prospect.getId());
        dto.setCompanyName(prospect.getCompanyName());
        dto.setCity(prospect.getCity());
        dto.setContactFirstName(prospect.getContactFirstName());
        dto.setContactLastName(prospect.getContactLastName());
        dto.setDesignation(prospect.getDesignation());
        dto.setEmail(prospect.getEmail());
        dto.setPhone(prospect.getPhone());
        dto.setReferredBy(prospect.getReferredBy());
        dto.setClassificationType(prospect.getClassificationType());
        dto.setProgramType(prospect.getProgramType());
        dto.setType(prospect.getType());
        dto.setAssociateId(prospect.getAssociateId());
        dto.setFirstMeetingDate(prospect.getFirstMeetingDate());
        dto.setLastMeetingDate(prospect.getLastMeetingDate());
        dto.setEntryDate(prospect.getEntryDate());
        dto.setProtectionStatus(prospect.getProtectionStatus());
        dto.setProtectionPeriodMonths(prospect.getProtectionPeriodMonths());
        dto.setDeletionStatus(prospect.getDeletionStatus());
        dto.setCreatedBy(prospect.getCreatedBy());
        dto.setCreatedAt(prospect.getCreatedAt());
        dto.setUpdatedAt(prospect.getUpdatedAt());
        return dto;
    }
}
