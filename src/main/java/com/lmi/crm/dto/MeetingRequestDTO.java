package com.lmi.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingRequestDTO {
    private Integer prospectId;
    private String pointOfContact;
    private String description;
    private LocalDateTime meetingAt;
}
