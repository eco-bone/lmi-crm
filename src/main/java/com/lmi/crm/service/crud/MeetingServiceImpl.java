package com.lmi.crm.service.crud;

import com.lmi.crm.dao.MeetingRepository;
import com.lmi.crm.dto.MeetingRequestDTO;
import com.lmi.crm.dto.MeetingResponseDTO;
import com.lmi.crm.entity.Meeting;
import com.lmi.crm.mapper.MeetingMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MeetingServiceImpl implements MeetingService {

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingMapper meetingMapper;

    @Override
    public MeetingResponseDTO createMeeting(MeetingRequestDTO request) {
        log.info("Creating new meeting for prospect ID {}", request.getProspectId());
        Meeting meeting = meetingMapper.toEntity(request);
        return meetingMapper.toDTO(meetingRepository.save(meeting));
    }

    @Override
    public MeetingResponseDTO getMeetingById(Integer id) {
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Meeting with ID {} not found", id);
                    return new EntityNotFoundException("Meeting not found with ID: " + id);
                });
        return meetingMapper.toDTO(meeting);
    }

    @Override
    public List<MeetingResponseDTO> getAllMeetings() {
        return meetingRepository.findAll()
                .stream()
                .map(meetingMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MeetingResponseDTO updateMeeting(Integer id, MeetingRequestDTO request) {
        log.info("Updating meeting with ID {}", id);
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Meeting with ID {} not found", id);
                    return new EntityNotFoundException("Meeting not found with ID: " + id);
                });
        meetingMapper.updateEntity(meeting, request);
        return meetingMapper.toDTO(meetingRepository.save(meeting));
    }

    @Override
    public void deleteMeeting(Integer id) {
        log.info("Deleting meeting with ID {}", id);
        Meeting meeting = meetingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Meeting with ID {} not found", id);
                    return new EntityNotFoundException("Meeting not found with ID: " + id);
                });
        meetingRepository.delete(meeting);
    }
}
