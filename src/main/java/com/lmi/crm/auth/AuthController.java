package com.lmi.crm.auth;

import com.lmi.crm.auth.dto.LoginRequest;
import com.lmi.crm.auth.dto.LoginResponse;
import com.lmi.crm.auth.dto.SetupPasswordRequest;
import com.lmi.crm.auth.dto.TokenValidationResponse;
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
        log.info("POST /api/auth/login — identifier: {}", request.getIdentifier());
        LoginResponse response = authService.login(request);
        log.info("POST /api/auth/login — success — userId: {}, role: {}", response.getUserId(), response.getRole());
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/setup/password")
    public ResponseEntity<ApiResponse<String>> setupPassword(
            @RequestParam String token,
            @Valid @RequestBody SetupPasswordRequest request) {
        log.info("POST /api/auth/setup/password — token: {}...{}", token.substring(0, 6), token.substring(token.length() - 4));
        String result = authService.setupPassword(token, request);
        log.info("POST /api/auth/setup/password — password set successfully");
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/setup/verify-email")
    public ResponseEntity<ApiResponse<String>> sendEmailOtp(@RequestParam Integer userId) {
        log.info("POST /api/auth/setup/verify-email — userId: {}", userId);
        String result = authService.sendEmailOtp(userId);
        log.info("POST /api/auth/setup/verify-email — OTP dispatched — userId: {}", userId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/setup/verify-phone")
    public ResponseEntity<ApiResponse<String>> sendPhoneOtp(@RequestParam Integer userId) {
        log.info("POST /api/auth/setup/verify-phone — userId: {}", userId);
        String result = authService.sendPhoneOtp(userId);
        log.info("POST /api/auth/setup/verify-phone — OTP dispatched — userId: {}", userId);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @PostMapping("/setup/otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("POST /api/auth/setup/otp — userId: {}, type: {}", request.getUserId(), request.getType());
        String result = authService.verifyOtp(request);
        log.info("POST /api/auth/setup/otp — result: {} — userId: {}", result, request.getUserId());
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }

    @GetMapping("/setup/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam String token) {
        log.debug("GET /api/auth/setup/validate-token — token: {}...{}", token.substring(0, 6), token.substring(token.length() - 4));
        TokenValidationResponse response = authService.validateInviteToken(token);
        log.debug("GET /api/auth/setup/validate-token — valid: {}", response.getValid());
        return ResponseEntity.ok(response);
    }
}
