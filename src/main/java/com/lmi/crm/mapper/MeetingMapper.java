package com.lmi.crm.mapper;

import com.lmi.crm.dto.request.AddMeetingRequest;
import com.lmi.crm.dto.response.MeetingResponse;
import com.lmi.crm.entity.Meeting;
import org.springframework.stereotype.Component;

@Component
public class MeetingMapper {

    public Meeting toEntity(AddMeetingRequest request, Integer createdBy) {
        return Meeting.builder()
                .prospectId(request.getProspectId())
                .pointOfContact(request.getPointOfContact())
                .description(request.getDescription())
                .meetingAt(request.getMeetingAt())
                .createdBy(createdBy)
                .build();
    }

    public MeetingResponse toResponse(Meeting meeting) {
        return MeetingResponse.builder()
                .id(meeting.getId())
                .prospectId(meeting.getProspectId())
                .pointOfContact(meeting.getPointOfContact())
                .description(meeting.getDescription())
                .meetingAt(meeting.getMeetingAt())
                .createdBy(meeting.getCreatedBy())
                .createdAt(meeting.getCreatedAt())
                .updatedAt(meeting.getUpdatedAt())
                .build();
    }
}
