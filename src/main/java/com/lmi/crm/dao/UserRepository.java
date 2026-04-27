package com.lmi.crm.dao;

import com.lmi.crm.entity.User;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByInvitationToken(String invitationToken);

    List<User> findByRole(UserRole role);

    @Query("SELECT u FROM User u WHERE u.licenseeId = :licenseeId AND u.role = :role AND (:status IS NULL OR u.status = :status)")
    List<User> findAssociatesByLicensee(@Param("licenseeId") Integer licenseeId,
                                        @Param("role") UserRole role,
                                        @Param("status") UserStatus status);

    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (:status IS NULL OR u.status = :status)")
    List<User> findByOptionalFilters(@Param("role") UserRole role,
                                     @Param("status") UserStatus status);

    long countByRole(UserRole role);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    long countByLicenseeIdAndRole(Integer licenseeId, UserRole role);

    long countByLicenseeIdAndRoleAndStatus(Integer licenseeId, UserRole role, UserStatus status);
}
