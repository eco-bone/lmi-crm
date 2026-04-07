package com.lmi.crm.mapper;

import com.lmi.crm.dto.MeetingRequestDTO;
import com.lmi.crm.dto.MeetingResponseDTO;
import com.lmi.crm.entity.Meeting;
import org.springframework.stereotype.Component;

@Component
public class MeetingMapper {

    public Meeting toEntity(MeetingRequestDTO dto) {
        Meeting meeting = new Meeting();
        meeting.setProspectId(dto.getProspectId());
        meeting.setPointOfContact(dto.getPointOfContact());
        meeting.setDescription(dto.getDescription());
        meeting.setMeetingAt(dto.getMeetingAt());
        return meeting;
    }

    public void updateEntity(Meeting meeting, MeetingRequestDTO dto) {
        meeting.setProspectId(dto.getProspectId());
        meeting.setPointOfContact(dto.getPointOfContact());
        meeting.setDescription(dto.getDescription());
        meeting.setMeetingAt(dto.getMeetingAt());
    }

    public MeetingResponseDTO toDTO(Meeting meeting) {
        MeetingResponseDTO dto = new MeetingResponseDTO();
        dto.setId(meeting.getId());
        dto.setProspectId(meeting.getProspectId());
        dto.setPointOfContact(meeting.getPointOfContact());
        dto.setDescription(meeting.getDescription());
        dto.setMeetingAt(meeting.getMeetingAt());
        dto.setCreatedBy(meeting.getCreatedBy());
        dto.setCreatedAt(meeting.getCreatedAt());
        dto.setUpdatedAt(meeting.getUpdatedAt());
        return dto;
    }
}
