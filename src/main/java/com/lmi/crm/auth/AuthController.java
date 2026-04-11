package com.lmi.crm.auth;

import com.lmi.crm.auth.dto.LoginRequest;
import com.lmi.crm.auth.dto.LoginResponse;
import com.lmi.crm.auth.dto.SetupPasswordRequest;
import com.lmi.crm.auth.dto.VerifyOtpRequest;
import com.lmi.crm.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/setup/password")
    public ResponseEntity<ApiResponse<String>> setupPassword(
            @RequestParam String token,
            @Valid @RequestBody SetupPasswordRequest request) {
        String result = authService.setupPassword(token, request);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/setup/verify-email")
    public ResponseEntity<ApiResponse<String>> sendEmailOtp(@RequestParam Integer userId) {
        String result = authService.sendEmailOtp(userId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/setup/verify-phone")
    public ResponseEntity<ApiResponse<String>> sendPhoneOtp(@RequestParam Integer userId) {
        String result = authService.sendPhoneOtp(userId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/setup/otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String result = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @GetMapping("/setup/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validateToken(@RequestParam String token) {
        Boolean valid = authService.validateInviteToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token validation result", valid));
    }
}
