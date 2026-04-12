package com.lmi.crm.util;

import com.lmi.crm.entity.User;
import com.lmi.crm.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public static Integer getCurrentUserId() {
        User user = getCurrentUser();
        return user.getId();
    }

    public static UserRole getCurrentUserRole() {
        User user = getCurrentUser();
        return user.getRole();
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new RuntimeException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }
}
