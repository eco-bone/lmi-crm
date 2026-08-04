package com.lmi.crm.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class MultipartDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            log.info("Multipart request received — {} {} — Content-Type: {}, Content-Length: {}",
                    request.getMethod(), request.getRequestURI(), contentType, request.getContentLengthLong());
            try {
                for (Part part : request.getParts()) {
                    log.info("Multipart part — name: '{}', submittedFileName: '{}', contentType: {}, size: {} bytes",
                            part.getName(), part.getSubmittedFileName(), part.getContentType(), part.getSize());
                }
            } catch (Exception ex) {
                log.error("Failed to parse multipart parts for {} {} — {}: {}",
                        request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
            }
        } else if (request.getRequestURI().endsWith("/import")) {
            log.warn("Request to {} did not have a multipart Content-Type — actual Content-Type: {}",
                    request.getRequestURI(), contentType);
        }
        filterChain.doFilter(request, response);
    }
}
