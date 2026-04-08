package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/licensees")
    public ResponseEntity<LicenseeResponse> addLicensee(
            @Valid @RequestBody AddLicenseeRequest request,
            @RequestParam Integer requestingUserId) {
        return ResponseEntity.ok(userService.addLicensee(request, requestingUserId));
    }
}
