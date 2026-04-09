package com.lmi.crm.dto.response;

import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;
    private Integer licenseeId;
    private UserStatus status;
    private LocalDateTime createdAt;
    private List<LicenseeResponse.CityInfo> cities;
}
