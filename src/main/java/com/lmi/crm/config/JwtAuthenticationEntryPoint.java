package com.lmi.crm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmi.crm.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Returns 401 + standard ApiResponse JSON when an unauthenticated request hits a
 * protected endpoint (expired/malformed/missing token, or token for a deleted user).
 * Without this, Spring Security's default sends 403 with an empty body.
 *
 * The frontend axios interceptor matches on this exact message to trigger
 * the redirect to /login — keep it in sync with src/lib/axios.js.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String SESSION_EXPIRED_MESSAGE = "Session expired or invalid";

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("Unauthenticated request rejected — {} {}", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(SESSION_EXPIRED_MESSAGE));
    }
}
