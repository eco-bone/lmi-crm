package com.lmi.crm.config;

import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("JWT filter — {} {}", method, uri);

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("JWT filter — no Bearer token, continuing as anonymous — {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtUtil.isValid(token)) {
            log.warn("JWT filter — invalid token rejected — {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        Integer userId = jwtUtil.extractUserId(token);
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            log.warn("JWT filter — token references non-existent userId: {} — {} {}", userId, method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        User user = userOpt.get();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT filter — authenticated userId: {}, role: {} — {} {}", user.getId(), user.getRole(), method, uri);

        filterChain.doFilter(request, response);
    }
}
