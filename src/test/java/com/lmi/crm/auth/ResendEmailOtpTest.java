package com.lmi.crm.auth;

import com.lmi.crm.auth.dto.ResendOtpRequest;
import com.lmi.crm.auth.dto.ResendOtpResponse;
import com.lmi.crm.dao.OtpStoreRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.entity.OtpStore;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.OtpType;
import com.lmi.crm.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendEmailOtpTest {

    @Mock UserRepository userRepository;
    @Mock OtpStoreRepository otpStoreRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks AuthServiceImpl authService;

    private User pendingUser() {
        User u = new User();
        u.setId(35);
        u.setEmail("jain2604sunil@gmail.com");
        u.setStatus(UserStatus.PENDING);
        return u;
    }

    @Test
    void sendsNewOtpForPendingUserWhenNoRecentOtp() {
        User u = pendingUser();
        when(userRepository.findByEmailIgnoreCase("jain2604sunil@gmail.com")).thenReturn(Optional.of(u));
        when(userRepository.findById(35)).thenReturn(Optional.of(u));
        OtpStore expired = OtpStore.builder().userId(35).type(OtpType.EMAIL)
                .createdAt(LocalDateTime.now().minusDays(1)).build();
        when(otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(35, OtpType.EMAIL)).thenReturn(Optional.of(expired));

        ResendOtpResponse r = authService.resendEmailOtp(new ResendOtpRequest("  Jain2604Sunil@gmail.com "));

        assertEquals(35, r.getUserId());
        verify(otpStoreRepository).deleteByUserIdAndType(35, OtpType.EMAIL);
        ArgumentCaptor<OtpStore> cap = ArgumentCaptor.forClass(OtpStore.class);
        verify(otpStoreRepository).save(cap.capture());
        assertEquals(6, cap.getValue().getOtp().length());
        assertTrue(cap.getValue().getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(4)));
        verify(eventPublisher).publishEvent(any(com.lmi.crm.event.NotificationEvent.class));
    }

    @Test
    void cooldownBlocksSecondRequest() {
        User u = pendingUser();
        when(userRepository.findByEmailIgnoreCase("jain2604sunil@gmail.com")).thenReturn(Optional.of(u));
        OtpStore fresh = OtpStore.builder().userId(35).type(OtpType.EMAIL)
                .createdAt(LocalDateTime.now().minusSeconds(10)).build();
        when(otpStoreRepository.findByUserIdAndTypeAndVerifiedFalse(35, OtpType.EMAIL)).thenReturn(Optional.of(fresh));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.resendEmailOtp(new ResendOtpRequest("jain2604sunil@gmail.com")));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatusCode());
        verify(otpStoreRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void unknownEmailGetsGenericResponseAndNoOtp() {
        when(userRepository.findByEmailIgnoreCase("nobody@x.com")).thenReturn(Optional.empty());
        ResendOtpResponse r = authService.resendEmailOtp(new ResendOtpRequest("nobody@x.com"));
        assertNull(r.getUserId());
        assertNotNull(r.getMessage());
        verifyNoInteractions(otpStoreRepository, eventPublisher);
    }

    @Test
    void activeUserGetsGenericResponseAndNoOtp() {
        User u = pendingUser();
        u.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("jain2604sunil@gmail.com")).thenReturn(Optional.of(u));
        ResendOtpResponse r = authService.resendEmailOtp(new ResendOtpRequest("jain2604sunil@gmail.com"));
        assertNull(r.getUserId());
        verifyNoInteractions(otpStoreRepository, eventPublisher);
    }
}
