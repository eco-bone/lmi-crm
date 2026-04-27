package com.lmi.crm.service;

import com.lmi.crm.dto.request.AddMeetingRequest;
import com.lmi.crm.dto.request.UpdateMeetingRequest;
import com.lmi.crm.dto.response.MeetingResponse;

import java.util.List;

public interface MeetingService {

    MeetingResponse addMeeting(AddMeetingRequest request, Integer requestingUserId);

    List<MeetingResponse> getMeetings(Integer requestingUserId, Integer prospectId);

    MeetingResponse getMeetingDetail(Integer requestingUserId, Integer meetingId);

    MeetingResponse updateMeeting(Integer requestingUserId, Integer meetingId, UpdateMeetingRequest request);

    String deleteMeeting(Integer requestingUserId, Integer meetingId);
}
