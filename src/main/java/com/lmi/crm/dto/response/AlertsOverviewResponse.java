package com.lmi.crm.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertsOverviewResponse {
    private long overallTotal;
    private long overallPending;
    private List<AlertSummaryEntry> summaries;
}
