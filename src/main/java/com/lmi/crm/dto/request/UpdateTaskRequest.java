package com.lmi.crm.dto.request;

import com.lmi.crm.enums.TaskStatus;
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
public class UpdateTaskRequest {

    @Size(min = 1)
    private String title;

    private String description;

    private LocalDateTime dueDate;

    private TaskStatus status;
}
