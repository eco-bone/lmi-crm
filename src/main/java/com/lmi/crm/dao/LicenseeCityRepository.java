package com.lmi.crm.dao;

import com.lmi.crm.entity.LicenseeCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenseeCityRepository extends JpaRepository<LicenseeCity, Integer> {
    void deleteByLicenseeId(Integer licenseeId);
}
