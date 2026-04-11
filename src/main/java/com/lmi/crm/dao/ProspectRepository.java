package com.lmi.crm.dao;

import com.lmi.crm.entity.Prospect;
import com.lmi.crm.enums.ProspectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, Integer> {

    List<Prospect> findByDeletionStatusFalse();

    Optional<Prospect> findByContactFirstNameIgnoreCaseAndContactLastNameIgnoreCaseAndCompanyNameIgnoreCaseAndDeletionStatusFalse(
            String contactFirstName, String contactLastName, String companyName);

    List<Prospect> findByCompanyNameStartingWithIgnoreCaseAndDeletionStatusFalse(String prefix);

    List<Prospect> findByAssociateIdAndDeletionStatusFalse(Integer associateId);

    List<Prospect> findByIdInAndDeletionStatusFalse(List<Integer> ids);

    List<Prospect> findByDeletionStatusFalseAndType(ProspectType type);

    List<Prospect> findByAssociateIdAndDeletionStatusFalseAndType(Integer associateId, ProspectType type);
}
