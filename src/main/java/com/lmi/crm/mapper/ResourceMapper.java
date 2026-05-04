package com.lmi.crm.mapper;

import com.lmi.crm.dto.response.ResourceResponse;
import com.lmi.crm.entity.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .resourceType(resource.getResourceType())
                .fileType(resource.getFileType())
                .fileUrl(resource.getFileUrl())
                .uploadedBy(resource.getUploadedBy())
                .createdAt(resource.getCreatedAt())
                .updatedAt(resource.getUpdatedAt())
                .build();
    }
}
