package com.lmi.crm.dao;

import com.lmi.crm.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Integer> {

    List<Meeting> findByProspectIdOrderByMeetingAtDesc(Integer prospectId);

    Optional<Meeting> findTopByProspectIdOrderByMeetingAtAsc(Integer prospectId);

    Optional<Meeting> findTopByProspectIdOrderByMeetingAtDesc(Integer prospectId);
}
