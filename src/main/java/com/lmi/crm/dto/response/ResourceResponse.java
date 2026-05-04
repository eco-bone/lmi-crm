package com.lmi.crm.dto.response;

import com.lmi.crm.enums.FileType;
import com.lmi.crm.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    private Integer id;
    private String title;
    private String description;
    private ResourceType resourceType;
    private FileType fileType;
    private String fileUrl;
    private Integer uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
