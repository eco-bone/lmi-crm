package com.lmi.crm.mapper;

import com.lmi.crm.dto.request.AddProspectRequest;
import com.lmi.crm.dto.response.ProspectResponse;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.ProspectLicensee;
import com.lmi.crm.enums.ProspectProgramType;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import com.lmi.crm.enums.ProtectionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
public class ProspectMapper {

    public Prospect fromAddProspectRequest(AddProspectRequest request, Integer associateId,
                                           Integer requestingUserId, boolean isProvisional) {
        Integer protectionPeriodMonths = null;
        if (request.getProgramType() == ProspectProgramType.LI) {
            protectionPeriodMonths = 12;
        } else if (request.getProgramType() == ProspectProgramType.SI) {
            protectionPeriodMonths = 6;
        }

        return Prospect.builder()
                .companyName(request.getCompanyName())
                .city(request.getCity())
                .contactFirstName(request.getContactFirstName())
                .contactLastName(request.getContactLastName())
                .designation(request.getDesignation())
                .email(request.getEmail())
                .phone(request.getPhone())
                .referredBy(request.getReferredBy())
                .classificationType(request.getClassificationType())
                .programType(request.getProgramType())
                .type(ProspectType.PROSPECT)
                .associateId(associateId)
                .protectionStatus(ProtectionStatus.PROTECTED)
                .status(isProvisional ? ProspectStatus.PROVISIONAL : ProspectStatus.NORMAL)
                .entryDate(LocalDate.now())
                .deletionStatus(false)
                .createdBy(requestingUserId)
                .protectionPeriodMonths(protectionPeriodMonths)
                .build();
    }

    public ProspectLicensee toProspectLicensee(Integer prospectId, Integer licenseeId) {
        return ProspectLicensee.builder()
                .prospectId(prospectId)
                .licenseeId(licenseeId)
                .isPrimary(true)
                .assignedAt(LocalDateTime.now())
                .build();
    }

    public ProspectResponse toLimitedResponse(Prospect prospect) {
        ProspectResponse response = new ProspectResponse();
        response.setId(prospect.getId());
        response.setCompanyName(prospect.getCompanyName());
        response.setCity(prospect.getCity());
        response.setContactFirstName(prospect.getContactFirstName());
        response.setContactLastName(prospect.getContactLastName());
        response.setDesignation(prospect.getDesignation());
        response.setType(prospect.getType());
        response.setProtectionStatus(prospect.getProtectionStatus());
        response.setProspectStatus(prospect.getStatus());
        return response;
    }

    public ProspectResponse toResponse(Prospect prospect, Integer licenseeId, String provisionReason) {
        ProspectResponse response = new ProspectResponse();
        response.setId(prospect.getId());
        response.setCompanyName(prospect.getCompanyName());
        response.setCity(prospect.getCity());
        response.setContactFirstName(prospect.getContactFirstName());
        response.setContactLastName(prospect.getContactLastName());
        response.setDesignation(prospect.getDesignation());
        response.setEmail(prospect.getEmail());
        response.setPhone(prospect.getPhone());
        response.setReferredBy(prospect.getReferredBy());
        response.setClassificationType(prospect.getClassificationType());
        response.setProgramType(prospect.getProgramType());
        response.setType(prospect.getType());
        response.setProtectionStatus(prospect.getProtectionStatus());
        response.setProspectStatus(prospect.getStatus());
        response.setProtectionPeriodMonths(prospect.getProtectionPeriodMonths());
        response.setEntryDate(prospect.getEntryDate());
        response.setAssociateId(prospect.getAssociateId());
        response.setLicenseeId(licenseeId);
        response.setCreatedBy(prospect.getCreatedBy());
        response.setCreatedAt(prospect.getCreatedAt());
        response.setProvisionReason(provisionReason);
        return response;
    }
}
