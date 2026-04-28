package com.lmi.crm.dto.response;

import com.lmi.crm.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryEntry {
    private AlertType alertType;
    private long totalCount;
    private long pendingCount;
    private Page<AlertResponse> firstPage;
}
