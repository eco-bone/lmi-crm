package com.lmi.crm.dto;

import com.lmi.crm.enums.FileType;
import com.lmi.crm.enums.ResourceType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResourceResponseDTO {
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
