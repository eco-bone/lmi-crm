package com.lmi.crm.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCityRequest {

    @NotBlank
    private String city;

    private boolean delete = false;
}
