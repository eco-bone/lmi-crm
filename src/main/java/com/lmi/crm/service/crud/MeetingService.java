package com.lmi.crm.service.crud;

import com.lmi.crm.dto.MeetingRequestDTO;
import com.lmi.crm.dto.MeetingResponseDTO;

import java.util.List;

public interface MeetingService {
    MeetingResponseDTO createMeeting(MeetingRequestDTO request);
    MeetingResponseDTO getMeetingById(Integer id);
    List<MeetingResponseDTO> getAllMeetings();
    MeetingResponseDTO updateMeeting(Integer id, MeetingRequestDTO request);
    void deleteMeeting(Integer id);
}
