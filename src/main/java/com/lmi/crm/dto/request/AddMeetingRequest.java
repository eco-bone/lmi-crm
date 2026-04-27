package com.lmi.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMeetingRequest {

    @NotNull
    private Integer prospectId;

    @NotBlank
    private String pointOfContact;

    private String description;

    @NotNull
    private LocalDateTime meetingAt;
}
