package com.lmi.crm.dto.response;

import com.lmi.crm.enums.AlertType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertSummaryResponse {

    private long total;
    private Map<AlertType, Long> byType;
}
