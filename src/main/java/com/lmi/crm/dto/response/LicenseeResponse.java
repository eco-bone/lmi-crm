package com.lmi.crm.dto.response;

import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LicenseeResponse {

    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private List<CityInfo> cities;
    private LocalDateTime createdAt;

    @Data
    public static class CityInfo {
        private Integer id;
        private String city;
        private Boolean isPrimary;
    }
}
