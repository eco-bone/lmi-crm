package com.lmi.crm.dao;

import com.lmi.crm.entity.GroupProspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface GroupProspectRepository extends JpaRepository<GroupProspect, Integer> {

    List<GroupProspect> findByGroupId(Integer groupId);

    List<GroupProspect> findByGroupIdAndProspectId(Integer groupId, Integer prospectId);

    boolean existsByGroupIdAndProspectId(Integer groupId, Integer prospectId);

    @Transactional
    void deleteByGroupIdAndProspectId(Integer groupId, Integer prospectId);
}
