package com.lmi.crm.auth;

import com.lmi.crm.config.JwtUtil;
import com.lmi.crm.dao.OtpStoreRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.auth.dto.LoginRequest;
import com.lmi.crm.auth.dto.LoginResponse;
import com.lmi.crm.auth.dto.SetupPasswordRequest;
import com.lmi.crm.auth.dto.VerifyOtpRequest;
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
        User user = userRepository.findByEmail(request.getIdentifier())
                .or(() -> userRepository.findByPhone(request.getIdentifier()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getStatus() == UserStatus.PENDING)
            throw new RuntimeException("Account setup not complete. Please check your invitation email.");

        if (user.getStatus() == UserStatus.INACTIVE)
            throw new RuntimeException("Account has been deactivated. Contact your administrator.");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtUtil.generateToken(user);

        log.info("User logged in — id: {}, role: {}", user.getId(), user.getRole());

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
        User user = userRepository.findByInvitationToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invitation token"));

        if (user.getStatus() != UserStatus.PENDING)
            throw new RuntimeException("This invitation has already been used");

        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new RuntimeException("Passwords do not match");

        if (request.getNewPassword().length() < 8)
            throw new RuntimeException("Password must be at least 8 characters");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setInvitationToken(null);
        userRepository.save(user);

        log.info("Password set via invite token — userId: {}", user.getId());

        return "Password set successfully. Please verify your email and phone to activate your account.";
    }

    @Override
    @Transactional
    public String sendEmailOtp(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.PENDING)
            throw new RuntimeException("Account is already active or deactivated");

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

        notificationService.sendOtpEmail(user.getEmail(), otp);

        log.info("Email OTP sent — userId: {}", userId);

        return "OTP sent to " + user.getEmail();
    }

    @Override
    @Transactional
    public String sendPhoneOtp(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (user.getStatus() != UserStatus.PENDING)
            throw new RuntimeException("Account is already active or deactivated");

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

        notificationService.sendOtpSms(user.getPhone(), otp);

        log.info("Phone OTP sent — userId: {}", userId);

        return "OTP sent to " + user.getPhone();
    }

    @Override
    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        Integer userId = request.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        OtpStore otpRecord = otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(userId, request.getType())
                .orElseThrow(() -> new RuntimeException("No pending OTP found"));

        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP has expired");

        if (!otpRecord.getOtp().equals(request.getOtp()))
            throw new RuntimeException("Invalid OTP");

        otpRecord.setVerified(true);
        otpStoreRepository.save(otpRecord);

        boolean emailVerified = otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(userId, OtpType.EMAIL).isEmpty()
                && otpStoreRepository.existsByUserIdAndTypeAndVerifiedTrue(userId, OtpType.EMAIL);
        boolean phoneVerified = otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(userId, OtpType.PHONE).isEmpty()
                && otpStoreRepository.existsByUserIdAndTypeAndVerifiedTrue(userId, OtpType.PHONE);

        if (emailVerified && phoneVerified) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            otpStoreRepository.deleteByUserId(userId);
            log.info("Account activated — userId: {}", userId);
            return "Account activated successfully. You can now log in.";
        }

        return "OTP verified successfully.";
    }

    @Override
    public Boolean validateInviteToken(String token) {
        return userRepository.findByInvitationToken(token).isPresent();
    }
}
