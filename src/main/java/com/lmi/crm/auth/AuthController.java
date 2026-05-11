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
        try {
            log.info("POST /api/auth/login — identifier: {}", request.getIdentifier());
            LoginResponse response = authService.login(request);
            log.info("POST /api/auth/login — success — userId: {}, role: {}", response.getUserId(), response.getRole());
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (RuntimeException ex) {
            log.error("POST /api/auth/login — failed — identifier: {} — {}", request.getIdentifier(), ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/auth/login — unexpected error — identifier: {}", request.getIdentifier(), ex);
            throw ex;
        }
    }

    @PostMapping("/setup/password")
    public ResponseEntity<ApiResponse<String>> setupPassword(
            @RequestParam String token,
            @Valid @RequestBody SetupPasswordRequest request) {
        try {
            String tokenPrefix = token.length() >= 10 ? token.substring(0, 6) + "..." + token.substring(token.length() - 4) : "[short-token]";
            log.info("POST /api/auth/setup/password — token: {}", tokenPrefix);
            String result = authService.setupPassword(token, request);
            log.info("POST /api/auth/setup/password — password set successfully");
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("POST /api/auth/setup/password — failed — {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/auth/setup/password — unexpected error", ex);
            throw ex;
        }
    }

    @PostMapping("/setup/verify-email")
    public ResponseEntity<ApiResponse<String>> sendEmailOtp(@RequestParam Integer userId) {
        try {
            log.info("POST /api/auth/setup/verify-email — userId: {}", userId);
            String result = authService.sendEmailOtp(userId);
            log.info("POST /api/auth/setup/verify-email — OTP dispatched — userId: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("POST /api/auth/setup/verify-email — failed — userId: {} — {}", userId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/auth/setup/verify-email — unexpected error — userId: {}", userId, ex);
            throw ex;
        }
    }

    @PostMapping("/setup/verify-phone")
    public ResponseEntity<ApiResponse<String>> sendPhoneOtp(@RequestParam Integer userId) {
        try {
            log.info("POST /api/auth/setup/verify-phone — userId: {}", userId);
            String result = authService.sendPhoneOtp(userId);
            log.info("POST /api/auth/setup/verify-phone — OTP dispatched — userId: {}", userId);
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("POST /api/auth/setup/verify-phone — failed — userId: {} — {}", userId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/auth/setup/verify-phone — unexpected error — userId: {}", userId, ex);
            throw ex;
        }
    }

    @PostMapping("/setup/otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            log.info("POST /api/auth/setup/otp — userId: {}, type: {}", request.getUserId(), request.getType());
            String result = authService.verifyOtp(request);
            log.info("POST /api/auth/setup/otp — result: {} — userId: {}", result, request.getUserId());
            return ResponseEntity.ok(ApiResponse.success(result, null));
        } catch (RuntimeException ex) {
            log.error("POST /api/auth/setup/otp — failed — userId: {}, type: {} — {}", request.getUserId(), request.getType(), ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/auth/setup/otp — unexpected error — userId: {}, type: {}", request.getUserId(), request.getType(), ex);
            throw ex;
        }
    }

    @GetMapping("/setup/validate-token")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestParam String token) {
        try {
            String tokenPrefix = token.length() >= 10 ? token.substring(0, 6) + "..." + token.substring(token.length() - 4) : "[short-token]";
            log.debug("GET /api/auth/setup/validate-token — token: {}", tokenPrefix);
            TokenValidationResponse response = authService.validateInviteToken(token);
            log.debug("GET /api/auth/setup/validate-token — valid: {}", response.getValid());
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.error("GET /api/auth/setup/validate-token — failed — {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/auth/setup/validate-token — unexpected error", ex);
            throw ex;
        }
    }
}
