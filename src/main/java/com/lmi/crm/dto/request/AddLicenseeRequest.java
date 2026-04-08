package com.lmi.crm.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddLicenseeRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String phone;

    @NotEmpty
    @Valid
    private List<CityRequest> cities;

    @Data
    public static class CityRequest {

        @NotBlank
        private String city;

        @NotNull
        private Boolean isPrimary;
    }
}
