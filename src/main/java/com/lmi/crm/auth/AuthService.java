package com.lmi.crm.auth;

import com.lmi.crm.auth.dto.LoginRequest;
import com.lmi.crm.auth.dto.LoginResponse;
import com.lmi.crm.auth.dto.ResendOtpRequest;
import com.lmi.crm.auth.dto.ResendOtpResponse;
import com.lmi.crm.auth.dto.SetupPasswordRequest;
import com.lmi.crm.auth.dto.TokenValidationResponse;
import com.lmi.crm.auth.dto.VerifyOtpRequest;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    String setupPassword(String token, SetupPasswordRequest request);

    String sendEmailOtp(Integer userId);

    String sendPhoneOtp(Integer userId);

    ResendOtpResponse resendEmailOtp(ResendOtpRequest request);

    String verifyOtp(VerifyOtpRequest request);

    TokenValidationResponse validateInviteToken(String token);
}
