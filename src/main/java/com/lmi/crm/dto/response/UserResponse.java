package com.lmi.crm.dto.response;

import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
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
}
