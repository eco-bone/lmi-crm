package com.lmi.crm.dao;

import com.lmi.crm.entity.LicenseeCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LicenseeCityRepository extends JpaRepository<LicenseeCity, Integer> {
    List<LicenseeCity> findByLicenseeId(Integer licenseeId);
    void deleteByLicenseeId(Integer licenseeId);
}
