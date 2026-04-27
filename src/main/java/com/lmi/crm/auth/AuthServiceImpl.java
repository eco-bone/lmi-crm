package com.lmi.crm.auth;

import com.lmi.crm.auth.dto.LoginRequest;
import com.lmi.crm.auth.dto.LoginResponse;
import com.lmi.crm.auth.dto.SetupPasswordRequest;
import com.lmi.crm.auth.dto.TokenValidationResponse;
import com.lmi.crm.auth.dto.VerifyOtpRequest;
import com.lmi.crm.config.JwtUtil;
import com.lmi.crm.dao.OtpStoreRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.entity.OtpStore;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.OtpType;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpStoreRepository otpStoreRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.debug("login — attempting — identifier: {}", request.getIdentifier());

        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> {
                    log.debug("login — email not found, trying phone — identifier: {}", request.getIdentifier());
                    return userRepository.findByPhone(request.getIdentifier());
                })
                .orElseThrow(() -> {
                    log.warn("login — no user found for identifier: {}", request.getIdentifier());
                    return new RuntimeException("Invalid credentials");
                });

        log.debug("login — user found — userId: {}, role: {}, status: {}", user.getId(), user.getRole(), user.getStatus());

        if (user.getStatus() == UserStatus.PENDING) {
            log.warn("login — blocked — account setup incomplete — userId: {}", user.getId());
            throw new RuntimeException("Account setup not complete. Please check your invitation email.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("login — blocked — account inactive — userId: {}", user.getId());
            throw new RuntimeException("Account has been deactivated. Contact your administrator.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("login — failed — wrong password — userId: {}", user.getId());
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user);
        log.info("login — success — userId: {}, role: {}", user.getId(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole().name())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Override
    @Transactional
    public String setupPassword(String token, SetupPasswordRequest request) {
        log.debug("setupPassword — looking up invitation token");

        User user = userRepository.findByInvitationToken(token)
                .orElseThrow(() -> {
                    log.warn("setupPassword — invalid or expired token");
                    return new RuntimeException("Invalid or expired invitation token");
                });

        log.debug("setupPassword — token matched — userId: {}, status: {}", user.getId(), user.getStatus());

        if (user.getStatus() != UserStatus.PENDING) {
            log.warn("setupPassword — invitation already used — userId: {}, status: {}", user.getId(), user.getStatus());
            throw new RuntimeException("This invitation has already been used");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("setupPassword — passwords do not match — userId: {}", user.getId());
            throw new RuntimeException("Passwords do not match");
        }

        if (request.getNewPassword().length() < 8) {
            log.warn("setupPassword — password too short — userId: {}", user.getId());
            throw new RuntimeException("Password must be at least 8 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setInvitationToken(null);
        userRepository.save(user);

        log.info("setupPassword — password set, invitation token cleared — userId: {}", user.getId());

        return "Password set successfully. Please verify your email and phone to activate your account.";
    }

    @Override
    @Transactional
    public String sendEmailOtp(Integer userId) {
        log.debug("sendEmailOtp — userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        log.debug("sendEmailOtp — user found — email: {}, status: {}", user.getEmail(), user.getStatus());

        if (user.getStatus() != UserStatus.PENDING) {
            log.warn("sendEmailOtp — rejected — status is not PENDING — userId: {}, status: {}", userId, user.getStatus());
            throw new RuntimeException("Account is already active or deactivated");
        }

        log.debug("sendEmailOtp — deleting any existing unverified EMAIL OTP — userId: {}", userId);
        otpStoreRepository.deleteByUserIdAndType(userId, OtpType.EMAIL);

        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpStore otpStore = OtpStore.builder()
                .userId(userId)
                .otp(otp)
                .type(OtpType.EMAIL)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        otpStoreRepository.save(otpStore);

        log.debug("sendEmailOtp — OTP saved, sending email — userId: {}, email: {}", userId, user.getEmail());
        notificationService.sendOtpEmail(user.getEmail(), otp);

        log.info("sendEmailOtp — sent — userId: {}, email: {}", userId, user.getEmail());
        return "OTP sent to " + user.getEmail();
    }

    @Override
    @Transactional
    public String sendPhoneOtp(Integer userId) {
        log.debug("sendPhoneOtp — userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        log.debug("sendPhoneOtp — user found — phone: {}, status: {}", user.getPhone(), user.getStatus());

        if (user.getStatus() != UserStatus.PENDING) {
            log.warn("sendPhoneOtp — rejected — status is not PENDING — userId: {}, status: {}", userId, user.getStatus());
            throw new RuntimeException("Account is already active or deactivated");
        }

        log.debug("sendPhoneOtp — deleting any existing unverified PHONE OTP — userId: {}", userId);
        otpStoreRepository.deleteByUserIdAndType(userId, OtpType.PHONE);

        String otp = String.format("%06d", new Random().nextInt(999999));

        OtpStore otpStore = OtpStore.builder()
                .userId(userId)
                .otp(otp)
                .type(OtpType.PHONE)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .verified(false)
                .build();
        otpStoreRepository.save(otpStore);

        log.debug("sendPhoneOtp — OTP saved, sending SMS — userId: {}, phone: {}", userId, user.getPhone());
        notificationService.sendOtpSms(user.getPhone(), otp);

        log.info("sendPhoneOtp — sent — userId: {}, phone: {}", userId, user.getPhone());
        return "OTP sent to " + user.getPhone();
    }

    @Override
    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        Integer userId = request.getUserId();
        log.debug("verifyOtp — userId: {}, type: {}", userId, request.getType());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        log.debug("verifyOtp — user found — userId: {}, status: {}", userId, user.getStatus());

        OtpStore otpRecord = otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(userId, request.getType())
                .orElseThrow(() -> {
                    log.warn("verifyOtp — no pending OTP found — userId: {}, type: {}", userId, request.getType());
                    return new RuntimeException("No pending OTP found");
                });

        log.debug("verifyOtp — OTP record found — expiresAt: {}", otpRecord.getExpiresAt());

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("verifyOtp — OTP expired — userId: {}, type: {}, expiredAt: {}", userId, request.getType(), otpRecord.getExpiresAt());
            throw new RuntimeException("OTP has expired");
        }

        if (!otpRecord.getOtp().equals(request.getOtp())) {
            log.warn("verifyOtp — wrong OTP submitted — userId: {}, type: {}", userId, request.getType());
            throw new RuntimeException("Invalid OTP");
        }

        otpRecord.setVerified(true);
        otpStoreRepository.save(otpRecord);
        log.info("verifyOtp — OTP verified — userId: {}, type: {}", userId, request.getType());

        boolean emailVerified = otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(userId, OtpType.EMAIL).isEmpty()
                && otpStoreRepository.existsByUserIdAndTypeAndVerifiedTrue(userId, OtpType.EMAIL);
        boolean phoneVerified = otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(userId, OtpType.PHONE).isEmpty()
                && otpStoreRepository.existsByUserIdAndTypeAndVerifiedTrue(userId, OtpType.PHONE);

        log.debug("verifyOtp — verification state — userId: {}, emailVerified: {}, phoneVerified: {}", userId, emailVerified, phoneVerified);

        if (emailVerified && phoneVerified) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            otpStoreRepository.deleteByUserId(userId);
            log.info("verifyOtp — account activated, OTP records purged — userId: {}", userId);
            return "Account activated successfully. You can now log in.";
        }

        return "OTP verified successfully.";
    }

    @Override
    public TokenValidationResponse validateInviteToken(String token) {
        log.debug("validateInviteToken — checking token");

        return userRepository.findByInvitationToken(token)
                .filter(user -> user.getStatus() == UserStatus.PENDING)
                .map(user -> {
                    log.info("validateInviteToken — valid token for userId: {}", user.getId());
                    return TokenValidationResponse.builder()
                            .valid(true)
                            .userId(user.getId())
                            .email(user.getEmail())
                            .phone(user.getPhone())
                            .firstName(user.getFirstName())
                            .lastName(user.getLastName())
                            .build();
                })
                .orElseGet(() -> {
                    log.warn("validateInviteToken — invalid or expired token");
                    return TokenValidationResponse.builder()
                            .valid(false)
                            .message("Invalid or expired invitation token")
                            .build();
                });
    }
}
