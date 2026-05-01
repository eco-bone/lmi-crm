package com.lmi.crm.dto.response;

import com.lmi.crm.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersSummaryResponse {
    private long overallTotal;
    private long activeCount;
    private long inactiveCount;
    private Map<UserRole, Long> countByRole;
    private Page<UserResponse> firstPage;
}
