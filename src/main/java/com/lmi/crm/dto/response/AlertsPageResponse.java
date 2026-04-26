package com.lmi.crm.dto.response;

import com.lmi.crm.enums.AlertStatus;
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
public class AlertsPageResponse {
    private long overallTotal;
    private long overallPending;
    private AlertType alertType;
    private AlertStatus statusFilter;
    private Page<AlertResponse> alerts;
}
