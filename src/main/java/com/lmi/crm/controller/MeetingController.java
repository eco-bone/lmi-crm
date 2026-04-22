package com.lmi.crm.controller;

import com.lmi.crm.dto.request.AddMeetingRequest;
import com.lmi.crm.dto.request.UpdateMeetingRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.MeetingResponse;
import com.lmi.crm.service.MeetingService;
import com.lmi.crm.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @PostMapping
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE')")
    public ResponseEntity<ApiResponse<MeetingResponse>> addMeeting(@Valid @RequestBody AddMeetingRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        MeetingResponse response = meetingService.addMeeting(request, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Meeting added successfully", response));
    }

    @GetMapping("/prospect/{prospectId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMeetings(@PathVariable Integer prospectId) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        List<MeetingResponse> response = meetingService.getMeetings(requestingUserId, prospectId);
        return ResponseEntity.ok(ApiResponse.success("Meetings retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeetingDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        MeetingResponse response = meetingService.getMeetingDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Meeting retrieved successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateMeetingRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        MeetingResponse response = meetingService.updateMeeting(requestingUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Meeting updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LICENSEE') or hasRole('ASSOCIATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteMeeting(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String message = meetingService.deleteMeeting(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }
}
