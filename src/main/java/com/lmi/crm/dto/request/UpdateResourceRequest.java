package com.lmi.crm.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateResourceRequest {

    @Size(min = 1)
    private String title;

    private String description;

    private String videoUrl;

    private MultipartFile file;
}
