package com.lmi.crm.dto;

import com.lmi.crm.enums.UserRole;
import lombok.Data;

@Data
public class UserRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private UserRole role;
}
