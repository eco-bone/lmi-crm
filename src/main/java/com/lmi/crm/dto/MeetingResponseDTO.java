package com.lmi.crm.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MeetingResponseDTO {
    private Integer id;
    private Integer prospectId;
    private String pointOfContact;
    private String description;
    private LocalDateTime meetingAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
