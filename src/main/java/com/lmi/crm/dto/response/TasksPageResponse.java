package com.lmi.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TasksPageResponse {
    private long totalCount;
    private long pendingCount;
    private long completedCount;
    private Page<TaskResponse> tasks;
}
