package com.lmi.crm.service;

import com.lmi.crm.dao.MeetingRepository;
import com.lmi.crm.dao.ProspectLicenseeRepository;
import com.lmi.crm.dao.ProspectRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.AddMeetingRequest;
import com.lmi.crm.dto.request.UpdateMeetingRequest;
import com.lmi.crm.dto.response.MeetingResponse;
import com.lmi.crm.entity.Meeting;
import com.lmi.crm.entity.Prospect;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.UserRole;
import com.lmi.crm.mapper.MeetingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class MeetingServiceImpl implements MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private ProspectRepository prospectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProspectLicenseeRepository prospectLicenseeRepository;

    @Autowired
    private MeetingMapper meetingMapper;

    @Override
    @Transactional
    public MeetingResponse addMeeting(AddMeetingRequest request, Integer requestingUserId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (requestingUser.getRole() != UserRole.LICENSEE && requestingUser.getRole() != UserRole.ASSOCIATE) {
            throw new RuntimeException("Access denied");
        }

        Prospect prospect = prospectRepository.findById(request.getProspectId())
                .orElseThrow(() -> new RuntimeException("Prospect not found"));
        if (Boolean.TRUE.equals(prospect.getDeletionStatus())) {
            throw new RuntimeException("Prospect not found");
        }

        if (requestingUser.getRole() == UserRole.LICENSEE) {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospect.getId(), requestingUserId)) {
                log.warn("Access denied — licensee {} not linked to prospect {}", requestingUserId, prospect.getId());
                throw new RuntimeException("Access denied");
            }
        } else {
            if (!requestingUserId.equals(prospect.getAssociateId())) {
                log.warn("Access denied — associate {} does not own prospect {}", requestingUserId, prospect.getId());
                throw new RuntimeException("Access denied");
            }
        }

        Meeting meeting = meetingMapper.toEntity(request, requestingUserId);
        Meeting saved = meetingRepository.save(meeting);

        prospect.setLastMeetingDate(request.getMeetingAt().toLocalDate());
        if (prospect.getFirstMeetingDate() == null) {
            prospect.setFirstMeetingDate(request.getMeetingAt().toLocalDate());
        }
        prospectRepository.save(prospect);

        log.info("Meeting added — id: {}, prospectId: {}, createdBy: {}, meetingAt: {}",
                saved.getId(), prospect.getId(), requestingUserId, saved.getMeetingAt());
        return meetingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetings(Integer requestingUserId, Integer prospectId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Prospect prospect = prospectRepository.findById(prospectId)
                .orElseThrow(() -> new RuntimeException("Prospect not found"));
        if (Boolean.TRUE.equals(prospect.getDeletionStatus())) {
            throw new RuntimeException("Prospect not found");
        }

        checkOwnership(requestingUser, prospect, prospectId, requestingUserId);

        List<MeetingResponse> meetings = meetingRepository.findByProspectIdOrderByMeetingAtDesc(prospectId)
                .stream().map(meetingMapper::toResponse).toList();

        log.info("GET meetings — prospectId: {}, requestingUserId: {}, count: {}", prospectId, requestingUserId, meetings.size());
        return meetings;
    }

    @Override
    @Transactional(readOnly = true)
    public MeetingResponse getMeetingDetail(Integer requestingUserId, Integer meetingId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        Prospect prospect = prospectRepository.findById(meeting.getProspectId())
                .orElseThrow(() -> new RuntimeException("Prospect not found"));
        if (Boolean.TRUE.equals(prospect.getDeletionStatus())) {
            throw new RuntimeException("Prospect not found");
        }

        checkOwnership(requestingUser, prospect, prospect.getId(), requestingUserId);

        return meetingMapper.toResponse(meeting);
    }

    @Override
    @Transactional
    public MeetingResponse updateMeeting(Integer requestingUserId, Integer meetingId, UpdateMeetingRequest request) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        Prospect prospect = prospectRepository.findById(meeting.getProspectId())
                .orElseThrow(() -> new RuntimeException("Prospect not found"));
        if (Boolean.TRUE.equals(prospect.getDeletionStatus())) {
            throw new RuntimeException("Prospect not found");
        }

        checkOwnership(requestingUser, prospect, prospect.getId(), requestingUserId);

        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;
        if (!meeting.getCreatedBy().equals(requestingUserId) && !isAdmin) {
            throw new RuntimeException("You can only update your own meetings");
        }

        if (request.getPointOfContact() != null) meeting.setPointOfContact(request.getPointOfContact());
        if (request.getDescription() != null) meeting.setDescription(request.getDescription());
        if (request.getMeetingAt() != null) meeting.setMeetingAt(request.getMeetingAt());

        Meeting saved = meetingRepository.save(meeting);

        if (request.getMeetingAt() != null) {
            Meeting latestMeeting = meetingRepository
                    .findTopByProspectIdOrderByMeetingAtDesc(prospect.getId())
                    .orElse(null);
            if (latestMeeting != null) {
                prospect.setLastMeetingDate(latestMeeting.getMeetingAt().toLocalDate());
            }

            Meeting firstMeeting = meetingRepository
                    .findTopByProspectIdOrderByMeetingAtAsc(prospect.getId())
                    .orElse(null);
            if (firstMeeting != null) {
                prospect.setFirstMeetingDate(firstMeeting.getMeetingAt().toLocalDate());
            }

            prospectRepository.save(prospect);
        }

        log.info("Meeting updated — id: {}, updatedBy: {}", meetingId, requestingUserId);
        return meetingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public String deleteMeeting(Integer requestingUserId, Integer meetingId) {
        User requestingUser = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("Meeting not found"));

        Prospect prospect = prospectRepository.findById(meeting.getProspectId())
                .orElseThrow(() -> new RuntimeException("Prospect not found"));
        if (Boolean.TRUE.equals(prospect.getDeletionStatus())) {
            throw new RuntimeException("Prospect not found");
        }

        checkOwnership(requestingUser, prospect, prospect.getId(), requestingUserId);

        boolean isAdmin = requestingUser.getRole() == UserRole.ADMIN || requestingUser.getRole() == UserRole.SUPER_ADMIN;
        if (!meeting.getCreatedBy().equals(requestingUserId) && !isAdmin) {
            throw new RuntimeException("You can only delete your own meetings");
        }

        meetingRepository.delete(meeting);

        Meeting latestMeeting = meetingRepository
                .findTopByProspectIdOrderByMeetingAtDesc(prospect.getId())
                .orElse(null);
        prospect.setLastMeetingDate(latestMeeting != null ? latestMeeting.getMeetingAt().toLocalDate() : null);

        Meeting firstMeeting = meetingRepository
                .findTopByProspectIdOrderByMeetingAtAsc(prospect.getId())
                .orElse(null);
        prospect.setFirstMeetingDate(firstMeeting != null ? firstMeeting.getMeetingAt().toLocalDate() : null);

        prospectRepository.save(prospect);

        log.info("Meeting deleted — id: {}, deletedBy: {}", meetingId, requestingUserId);
        return "Meeting deleted successfully";
    }

    private void checkOwnership(User user, Prospect prospect, Integer prospectId, Integer userId) {
        UserRole role = user.getRole();
        if (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN) {
            return;
        }
        if (role == UserRole.ASSOCIATE) {
            if (!userId.equals(prospect.getAssociateId())) {
                log.warn("Access denied — associate {} does not own prospect {}", userId, prospectId);
                throw new RuntimeException("Access denied");
            }
        } else if (role == UserRole.LICENSEE) {
            if (!prospectLicenseeRepository.existsByProspectIdAndLicenseeId(prospectId, userId)) {
                log.warn("Access denied — licensee {} not linked to prospect {}", userId, prospectId);
                throw new RuntimeException("Access denied");
            }
        } else {
            throw new RuntimeException("Access denied");
        }
    }
}
