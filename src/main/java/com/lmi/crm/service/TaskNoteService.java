package com.lmi.crm.service;

import com.lmi.crm.dto.request.CreateNoteRequest;
import com.lmi.crm.dto.request.CreateTaskRequest;
import com.lmi.crm.dto.request.UpdateNoteRequest;
import com.lmi.crm.dto.request.UpdateTaskRequest;
import com.lmi.crm.dto.response.NoteResponse;
import com.lmi.crm.dto.response.TaskResponse;
import com.lmi.crm.enums.TaskStatus;

public interface TaskNoteService {

    // Tasks
    TaskResponse createTask(CreateTaskRequest request, Integer requestingUserId);
    TaskResponse updateTask(Integer requestingUserId, Integer taskId, UpdateTaskRequest request);
    String deleteTask(Integer requestingUserId, Integer taskId);
    Object getTasks(Integer requestingUserId, boolean getAll, TaskStatus statusFilter, int page, int limit);
    TaskResponse getTaskDetail(Integer requestingUserId, Integer taskId);

    // Notes
    NoteResponse createNote(CreateNoteRequest request, Integer requestingUserId);
    NoteResponse updateNote(Integer requestingUserId, Integer noteId, UpdateNoteRequest request);
    String deleteNote(Integer requestingUserId, Integer noteId);
    Object getNotes(Integer requestingUserId, boolean getAll, int page, int limit);
    NoteResponse getNoteDetail(Integer requestingUserId, Integer noteId);
}
