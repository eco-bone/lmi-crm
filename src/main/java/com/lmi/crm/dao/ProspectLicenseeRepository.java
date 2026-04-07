package com.lmi.crm.dao;

import com.lmi.crm.entity.ProspectLicensee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProspectLicenseeRepository extends JpaRepository<ProspectLicensee, Integer> {
}
