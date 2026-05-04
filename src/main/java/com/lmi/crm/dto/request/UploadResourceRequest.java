package com.lmi.crm.dto.request;

import com.lmi.crm.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadResourceRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private ResourceType resourceType;

    private String videoUrl;

    private MultipartFile file;
}
