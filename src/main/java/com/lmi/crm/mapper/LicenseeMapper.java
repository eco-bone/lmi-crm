package com.lmi.crm.mapper;

import com.lmi.crm.dto.response.LicenseeResponse;
import com.lmi.crm.entity.LicenseeCity;
import com.lmi.crm.entity.User;

import java.util.List;

public class LicenseeMapper {

    public static LicenseeResponse toResponse(User user, List<LicenseeCity> cities) {
        LicenseeResponse response = new LicenseeResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setCities(cities.stream().map(c -> {
            LicenseeResponse.CityInfo info = new LicenseeResponse.CityInfo();
            info.setId(c.getId());
            info.setCity(c.getCity());
            info.setIsPrimary(c.getIsPrimary());
            return info;
        }).toList());
        return response;
    }
}
