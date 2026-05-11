package com.lmi.crm.dto.request;

import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {

    @Size(min = 1)
    private String firstName;

    @Size(min = 1)
    private String lastName;

    @Size(min = 1)
    private String email;

    @Pattern(regexp = "\\d{10}", message = "Phone number must be exactly 10 digits")
    private String phone;

    private UserStatus status;

    private UserRole role;

    private Integer newLicenseeId;

    private String newPrimaryCity;

    @Valid
    private List<UpdateCityRequest> cities;
}
