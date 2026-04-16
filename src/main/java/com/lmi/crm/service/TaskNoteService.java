package com.lmi.crm.service;

import com.lmi.crm.dto.request.CreateNoteRequest;
import com.lmi.crm.dto.request.CreateTaskRequest;
import com.lmi.crm.dto.request.UpdateNoteRequest;
import com.lmi.crm.dto.request.UpdateTaskRequest;
import com.lmi.crm.dto.response.NoteResponse;
import com.lmi.crm.dto.response.TaskResponse;
import com.lmi.crm.enums.TaskStatus;

import java.util.List;

public interface TaskNoteService {

    // Tasks
    TaskResponse createTask(CreateTaskRequest request, Integer requestingUserId);
    TaskResponse updateTask(Integer requestingUserId, Integer taskId, UpdateTaskRequest request);
    String deleteTask(Integer requestingUserId, Integer taskId);
    List<TaskResponse> getTasks(Integer requestingUserId, TaskStatus statusFilter);
    TaskResponse getTaskDetail(Integer requestingUserId, Integer taskId);

    // Notes
    NoteResponse createNote(CreateNoteRequest request, Integer requestingUserId);
    NoteResponse updateNote(Integer requestingUserId, Integer noteId, UpdateNoteRequest request);
    String deleteNote(Integer requestingUserId, Integer noteId);
    List<NoteResponse> getNotes(Integer requestingUserId);
    NoteResponse getNoteDetail(Integer requestingUserId, Integer noteId);
}
