package com.lmi.crm.service;

import com.lmi.crm.dao.AuditLogRepository;
import com.lmi.crm.entity.AuditLog;
import com.lmi.crm.entity.Group;
import com.lmi.crm.entity.Meeting;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.Resource;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.AuditActionType;
import com.lmi.crm.enums.RelatedEntityType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async("auditExecutor")
    public void log(AuditActionType actionType, RelatedEntityType entityType, Integer entityId,
                    Integer performedBy, Map<String, Object> previousState,
                    Map<String, Object> newState, Map<String, Object> metadata) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .performedBy(performedBy)
                    .previousState(previousState)
                    .newState(newState)
                    .metadata(metadata)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log — action: {}, entityId: {} — {}", actionType, entityId, e.getMessage());
        }
    }

    public Map<String, Object> snapshot(Prospect p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("companyName", p.getCompanyName());
        m.put("city", p.getCity());
        m.put("contactFirstName", p.getContactFirstName());
        m.put("contactLastName", p.getContactLastName());
        m.put("email", p.getEmail());
        m.put("programType", p.getProgramType() != null ? p.getProgramType().name() : null);
        m.put("type", p.getType() != null ? p.getType().name() : null);
        m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
        m.put("associateId", p.getAssociateId());
        m.put("entryDate", p.getEntryDate() != null ? p.getEntryDate().toString() : null);
        m.put("firstMeetingDate", p.getFirstMeetingDate() != null ? p.getFirstMeetingDate().toString() : null);
        m.put("lastMeetingDate", p.getLastMeetingDate() != null ? p.getLastMeetingDate().toString() : null);
        m.put("protectionPeriodMonths", p.getProtectionPeriodMonths());
        m.put("deletionStatus", p.getDeletionStatus());
        return m;
    }

    public Map<String, Object> snapshot(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("firstName", u.getFirstName());
        m.put("lastName", u.getLastName());
        m.put("email", u.getEmail());
        m.put("role", u.getRole() != null ? u.getRole().name() : null);
        m.put("status", u.getStatus() != null ? u.getStatus().name() : null);
        m.put("licenseeId", u.getLicenseeId());
        return m;
    }

    public Map<String, Object> snapshot(Group g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("groupType", g.getGroupType() != null ? g.getGroupType().name() : null);
        m.put("groupSize", g.getGroupSize());
        m.put("deliveryType", g.getDeliveryType() != null ? g.getDeliveryType().name() : null);
        m.put("licenseeId", g.getLicenseeId());
        m.put("facilitatorId", g.getFacilitatorId());
        m.put("startDate", g.getStartDate() != null ? g.getStartDate().toString() : null);
        m.put("deletionStatus", g.getDeletionStatus());
        return m;
    }

    public Map<String, Object> snapshot(Meeting meeting) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", meeting.getId());
        m.put("prospectId", meeting.getProspectId());
        m.put("meetingAt", meeting.getMeetingAt() != null ? meeting.getMeetingAt().toString() : null);
        m.put("pointOfContact", meeting.getPointOfContact());
        m.put("createdBy", meeting.getCreatedBy());
        return m;
    }

    public Map<String, Object> snapshot(Resource r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("title", r.getTitle());
        m.put("resourceType", r.getResourceType() != null ? r.getResourceType().name() : null);
        m.put("fileType", r.getFileType() != null ? r.getFileType().name() : null);
        m.put("uploadedBy", r.getUploadedBy());
        m.put("deletionStatus", r.getDeletionStatus());
        return m;
    }
}
