package com.lmi.crm.dto;

import com.lmi.crm.enums.FileType;
import com.lmi.crm.enums.ResourceType;
import lombok.Data;

@Data
public class ResourceRequestDTO {
    private String title;
    private String description;
    private ResourceType resourceType;
    private FileType fileType;
    private String fileUrl;
}
