package com.lmi.crm.service;

import com.lmi.crm.dao.LicenseeCityRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddLicenseeRequest;
import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.entity.LicenseeCity;
import com.lmi.crm.entity.User;
import com.lmi.crm.mapper.LicenseeMapper;
import com.lmi.crm.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseeCityRepository licenseeCityRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserMapper userMapper;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public LicenseeResponse addLicensee(AddLicenseeRequest request, Integer requestingUserId) {
        boolean hasPrimary = request.getCities().stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsPrimary()));
        if (!hasPrimary)
            throw new IllegalArgumentException("At least one city must be marked as primary");

        if (userRepository.findByEmail(request.getEmail()).isPresent())
            throw new IllegalArgumentException("A user with this email already exists");

        String tempPassword = "Temp-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = userMapper.fromAddLicenseeRequest(request, tempPassword);
        user = userRepository.save(user);
        final Integer userId = user.getId();

        List<LicenseeCity> cities = request.getCities().stream()
                .map(c -> LicenseeCity.builder()
                        .licenseeId(userId)
                        .city(c.getCity())
                        .isPrimary(Boolean.TRUE.equals(c.getIsPrimary()))
                        .build())
                .toList();

        List<LicenseeCity> savedCities = licenseeCityRepository.saveAll(cities);

        // TODO: replace with real invitation token when auth is built
        String inviteLink = baseUrl + "/register?token=PENDING";
        notificationService.sendInviteEmail(user.getEmail(), inviteLink, tempPassword);

        log.info("Licensee created — id: {}, email: {}, createdBy: {}", userId, user.getEmail(), requestingUserId);

        return LicenseeMapper.toResponse(user, savedCities);
    }
}
