package com.lmi.crm.mapper;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User fromAddLicenseeRequest(AddLicenseeRequest request, String tempPassword) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(tempPassword)
                .role(UserRole.LICENSEE)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public User forAssociate(String firstName, String lastName, String email,
                             String phone, String tempPassword, Integer licenseeId) {
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .password(tempPassword)
                .role(UserRole.ASSOCIATE)
                .status(UserStatus.ACTIVE)
                .licenseeId(licenseeId)
                .build();
    }

    public User forAdmin(String firstName, String lastName, String email,
                         String phone, String tempPassword) {
        return User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .password(tempPassword)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
