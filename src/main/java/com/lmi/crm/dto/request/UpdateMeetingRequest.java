package com.lmi.crm.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMeetingRequest {

    @Size(min = 1)
    private String pointOfContact;

    private String description;

    private LocalDateTime meetingAt;
}
