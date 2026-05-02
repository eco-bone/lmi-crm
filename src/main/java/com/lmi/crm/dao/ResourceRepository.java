package com.lmi.crm.dao;

import com.lmi.crm.entity.Resource;
import com.lmi.crm.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Integer> {

    List<Resource> findByDeletionStatusFalse();

    List<Resource> findByResourceTypeAndDeletionStatusFalse(ResourceType type);

    Page<Resource> findByDeletionStatusFalse(Pageable pageable);

    Page<Resource> findByResourceTypeAndDeletionStatusFalse(ResourceType type, Pageable pageable);

    long countByDeletionStatusFalse();

    long countByResourceTypeAndDeletionStatusFalse(ResourceType type);

    Optional<Resource> findByFileUrlAndDeletionStatusFalse(String fileUrl);
}
