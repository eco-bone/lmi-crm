package com.lmi.crm.controller;

import com.lmi.crm.dto.request.CreateNoteRequest;
import com.lmi.crm.dto.request.CreateTaskRequest;
import com.lmi.crm.dto.request.UpdateNoteRequest;
import com.lmi.crm.dto.request.UpdateTaskRequest;
import com.lmi.crm.dto.response.ApiResponse;
import com.lmi.crm.dto.response.NoteResponse;
import com.lmi.crm.dto.response.TaskResponse;
import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.service.TaskNoteService;
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
@RequestMapping("/api")
public class TaskNoteController {

    @Autowired
    private TaskNoteService taskNoteService;

    // -------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------

    @PostMapping("/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskNoteService.createTask(request, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", response));
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskNoteService.updateTask(requestingUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteTask(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String message = taskNoteService.deleteTask(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @GetMapping("/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(
            @RequestParam(required = false) TaskStatus status) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        List<TaskResponse> response = taskNoteService.getTasks(requestingUserId, status);
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", response));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskNoteService.getTaskDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", response));
    }

    // -------------------------------------------------------------------------
    // Notes
    // -------------------------------------------------------------------------

    @PostMapping("/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody CreateNoteRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        NoteResponse response = taskNoteService.createNote(request, requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Note created successfully", response));
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateNoteRequest request) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        NoteResponse response = taskNoteService.updateNote(requestingUserId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Note updated successfully", response));
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteNote(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        String message = taskNoteService.deleteNote(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @GetMapping("/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> getNotes() {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        List<NoteResponse> response = taskNoteService.getNotes(requestingUserId);
        return ResponseEntity.ok(ApiResponse.success("Notes retrieved successfully", response));
    }

    @GetMapping("/notes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoteResponse>> getNoteDetail(@PathVariable Integer id) {
        Integer requestingUserId = SecurityUtils.getCurrentUserId();
        NoteResponse response = taskNoteService.getNoteDetail(requestingUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Note retrieved successfully", response));
    }
}
