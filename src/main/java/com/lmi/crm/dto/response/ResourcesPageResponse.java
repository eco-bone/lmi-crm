package com.lmi.crm.dto.response;

import com.lmi.crm.enums.ResourceType;
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
public class ResourcesPageResponse {
    private long totalCount;
    private Map<ResourceType, Long> countByType;
    private Page<ResourceResponse> resources;
}
