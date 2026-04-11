package com.lmi.crm.dao;

import com.lmi.crm.entity.ProspectLicensee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProspectLicenseeRepository extends JpaRepository<ProspectLicensee, Integer> {

    Optional<ProspectLicensee> findByProspectIdAndIsPrimaryTrue(Integer prospectId);

    boolean existsByProspectIdAndLicenseeId(Integer prospectId, Integer licenseeId);

    List<ProspectLicensee> findByLicenseeId(Integer licenseeId);

    List<ProspectLicensee> findByProspectIdInAndIsPrimaryTrue(List<Integer> prospectIds);
}
