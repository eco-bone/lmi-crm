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
public class ProspectsPageResponse {
    private long overallTotal;
    private long prospectCount;
    private long clientCount;
    private long provisionalCount;
    private long unprotectedCount;
    private Long globalTotal;
    private Long globalPending;
    private Page<ProspectResponse> prospects;
}
