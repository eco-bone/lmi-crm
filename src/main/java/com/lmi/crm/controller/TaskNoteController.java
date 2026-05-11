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
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            TaskResponse response = taskNoteService.createTask(request, requestingUserId);
            return ResponseEntity.ok(ApiResponse.success("Task created successfully", response));
        } catch (RuntimeException ex) {
            log.error("POST /api/tasks — failed — requestingUserId: {} — {}", requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/tasks — unexpected error — requestingUserId: {}", requestingUserId, ex);
            throw ex;
        }
    }

    @PutMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateTaskRequest request) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            TaskResponse response = taskNoteService.updateTask(requestingUserId, id, request);
            return ResponseEntity.ok(ApiResponse.success("Task updated successfully", response));
        } catch (RuntimeException ex) {
            log.error("PUT /api/tasks/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/tasks/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @DeleteMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteTask(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            String message = taskNoteService.deleteTask(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (RuntimeException ex) {
            log.error("DELETE /api/tasks/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("DELETE /api/tasks/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @GetMapping("/tasks")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getTasks(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            Object response = taskNoteService.getTasks(requestingUserId, getAll, status, page, limit);
            return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/tasks — failed — requestingUserId: {}, getAll: {}, status: {} — {}", requestingUserId, getAll, status, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/tasks — unexpected error — requestingUserId: {}, getAll: {}, status: {}", requestingUserId, getAll, status, ex);
            throw ex;
        }
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskDetail(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            TaskResponse response = taskNoteService.getTaskDetail(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/tasks/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/tasks/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Notes
    // -------------------------------------------------------------------------

    @PostMapping("/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoteResponse>> createNote(@Valid @RequestBody CreateNoteRequest request) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            NoteResponse response = taskNoteService.createNote(request, requestingUserId);
            return ResponseEntity.ok(ApiResponse.success("Note created successfully", response));
        } catch (RuntimeException ex) {
            log.error("POST /api/notes — failed — requestingUserId: {} — {}", requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("POST /api/notes — unexpected error — requestingUserId: {}", requestingUserId, ex);
            throw ex;
        }
    }

    @PutMapping("/notes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateNoteRequest request) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            NoteResponse response = taskNoteService.updateNote(requestingUserId, id, request);
            return ResponseEntity.ok(ApiResponse.success("Note updated successfully", response));
        } catch (RuntimeException ex) {
            log.error("PUT /api/notes/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("PUT /api/notes/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @DeleteMapping("/notes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> deleteNote(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            String message = taskNoteService.deleteNote(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success(message, null));
        } catch (RuntimeException ex) {
            log.error("DELETE /api/notes/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("DELETE /api/notes/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }

    @GetMapping("/notes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getNotes(
            @RequestParam(defaultValue = "false") boolean getAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int limit) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            Object response = taskNoteService.getNotes(requestingUserId, getAll, page, limit);
            return ResponseEntity.ok(ApiResponse.success("Notes retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/notes — failed — requestingUserId: {}, getAll: {} — {}", requestingUserId, getAll, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/notes — unexpected error — requestingUserId: {}, getAll: {}", requestingUserId, getAll, ex);
            throw ex;
        }
    }

    @GetMapping("/notes/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NoteResponse>> getNoteDetail(@PathVariable Integer id) {
        Integer requestingUserId = null;
        try {
            requestingUserId = SecurityUtils.getCurrentUserId();
            NoteResponse response = taskNoteService.getNoteDetail(requestingUserId, id);
            return ResponseEntity.ok(ApiResponse.success("Note retrieved successfully", response));
        } catch (RuntimeException ex) {
            log.error("GET /api/notes/{} — failed — requestingUserId: {} — {}", id, requestingUserId, ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("GET /api/notes/{} — unexpected error — requestingUserId: {}", id, requestingUserId, ex);
            throw ex;
        }
    }
}
