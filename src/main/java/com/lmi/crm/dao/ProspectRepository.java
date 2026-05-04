package com.lmi.crm.dao;

import com.lmi.crm.entity.Prospect;
import com.lmi.crm.enums.ProspectStatus;
import com.lmi.crm.enums.ProspectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProspectRepository extends JpaRepository<Prospect, Integer> {

    List<Prospect> findByDeletionStatusFalse();

    Optional<Prospect> findByContactFirstNameIgnoreCaseAndContactLastNameIgnoreCaseAndCompanyNameIgnoreCaseAndDeletionStatusFalse(
            String contactFirstName, String contactLastName, String companyName);

    Optional<Prospect> findByEmailIgnoreCaseAndDeletionStatusFalse(String email);

    List<Prospect> findByCompanyNameStartingWithIgnoreCaseAndDeletionStatusFalse(String prefix);

    List<Prospect> findByAssociateIdAndDeletionStatusFalse(Integer associateId);

    List<Prospect> findByIdInAndDeletionStatusFalse(List<Integer> ids);

    List<Prospect> findByDeletionStatusFalseAndType(ProspectType type);

    List<Prospect> findByAssociateIdAndDeletionStatusFalseAndType(Integer associateId, ProspectType type);

    long countByDeletionStatusFalse();

    long countByStatusAndDeletionStatusFalse(ProspectStatus status);

    long countByTypeAndDeletionStatusFalse(ProspectType type);

    @Query("SELECT p FROM Prospect p WHERE p.deletionStatus = false AND (LOWER(p.companyName) LIKE LOWER(:q) OR LOWER(p.contactFirstName) LIKE LOWER(:q) OR LOWER(p.contactLastName) LIKE LOWER(:q) OR LOWER(p.city) LIKE LOWER(:q) OR LOWER(p.email) LIKE LOWER(:q) OR LOWER(p.phone) LIKE LOWER(:q))")
    List<Prospect> searchAll(@Param("q") String q);

    @Query("SELECT p FROM Prospect p WHERE p.deletionStatus = false AND p.associateId = :associateId AND (LOWER(p.companyName) LIKE LOWER(:q) OR LOWER(p.contactFirstName) LIKE LOWER(:q) OR LOWER(p.contactLastName) LIKE LOWER(:q) OR LOWER(p.city) LIKE LOWER(:q) OR LOWER(p.email) LIKE LOWER(:q) OR LOWER(p.phone) LIKE LOWER(:q))")
    List<Prospect> searchByAssociateId(@Param("q") String q, @Param("associateId") Integer associateId);

    @Query("SELECT p FROM Prospect p WHERE p.deletionStatus = false AND p.id IN :ids AND (LOWER(p.companyName) LIKE LOWER(:q) OR LOWER(p.contactFirstName) LIKE LOWER(:q) OR LOWER(p.contactLastName) LIKE LOWER(:q) OR LOWER(p.city) LIKE LOWER(:q) OR LOWER(p.email) LIKE LOWER(:q) OR LOWER(p.phone) LIKE LOWER(:q))")
    List<Prospect> searchByIds(@Param("q") String q, @Param("ids") List<Integer> ids);

    @Query("SELECT p FROM Prospect p WHERE p.firstMeetingDate IS NULL AND p.entryDate <= :cutoff AND p.status = 'PROTECTED' AND p.deletionStatus = false AND p.programType NOT IN ('ONE_TO_ONE', 'SHC')")
    List<Prospect> findProtectedProspectsWithNoFirstMeeting(@Param("cutoff") LocalDate cutoff);

    @Query("SELECT p FROM Prospect p WHERE p.firstMeetingDate IS NOT NULL AND p.lastMeetingDate <= :cutoff AND p.status = 'PROTECTED' AND p.deletionStatus = false AND p.programType IN ('LI', 'SI')")
    List<Prospect> findInactiveProtectedProspects(@Param("cutoff") LocalDate cutoff);
}
