package com.lmi.crm.dao;

import com.lmi.crm.entity.OtpStore;
import com.lmi.crm.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface OtpStoreRepository extends JpaRepository<OtpStore, Integer> {

    Optional<OtpStore> findByUserIdAndTypeAndVerifiedFalse(Integer userId, OtpType type);

    @Transactional
    void deleteByUserIdAndType(Integer userId, OtpType type);

    @Transactional
    void deleteByUserId(Integer userId);

    boolean existsByUserIdAndTypeAndVerifiedTrue(Integer userId, OtpType type);
}
