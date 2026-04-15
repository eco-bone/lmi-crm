package com.lmi.crm.service;

import com.lmi.crm.dao.UserItemRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.CreateNoteRequest;
import com.lmi.crm.dto.request.CreateTaskRequest;
import com.lmi.crm.dto.request.UpdateNoteRequest;
import com.lmi.crm.dto.request.UpdateTaskRequest;
import com.lmi.crm.dto.response.NoteResponse;
import com.lmi.crm.dto.response.TaskResponse;
import com.lmi.crm.entity.UserItem;
import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.enums.UserItemType;
import com.lmi.crm.mapper.TaskNoteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TaskNoteServiceImpl implements TaskNoteService {

    @Autowired
    private UserItemRepository userItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskNoteMapper taskNoteMapper;

    // -------------------------------------------------------------------------
    // Tasks
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Integer requestingUserId) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!request.getDueDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Due date must be in the future");
        }

        UserItem saved = userItemRepository.save(taskNoteMapper.toTaskEntity(request, requestingUserId));
        log.info("Task created — id: {}, userId: {}, dueDate: {}", saved.getId(), requestingUserId, saved.getDueDate());
        return taskNoteMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Integer requestingUserId, Integer taskId, UpdateTaskRequest request) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserItem item = userItemRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (item.getType() != UserItemType.TASK) {
            throw new RuntimeException("Item is not a task");
        }
        if (!item.getUserId().equals(requestingUserId)) {
            log.warn("Task update denied — taskId: {}, requestingUserId: {}, ownerId: {}", taskId, requestingUserId, item.getUserId());
            throw new RuntimeException("Access denied");
        }

        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getDueDate() != null) {
            if (request.getDueDate().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Due date must be in the future");
            }
            item.setDueDate(request.getDueDate());
        }
        if (request.getStatus() != null) item.setTaskStatus(request.getStatus());

        UserItem saved = userItemRepository.save(item);
        log.info("Task updated — id: {}, userId: {}", taskId, requestingUserId);
        return taskNoteMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public String deleteTask(Integer requestingUserId, Integer taskId) {
        UserItem item = userItemRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (item.getType() != UserItemType.TASK || !item.getUserId().equals(requestingUserId)) {
            log.warn("Task delete denied — taskId: {}, requestingUserId: {}", taskId, requestingUserId);
            throw new RuntimeException("Access denied");
        }

        userItemRepository.delete(item);
        log.info("Task deleted — id: {}, userId: {}", taskId, requestingUserId);
        return "Task deleted successfully";
    }

    @Override
    public List<TaskResponse> getTasks(Integer requestingUserId, TaskStatus statusFilter) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserItem> tasks;
        if (statusFilter != null) {
            tasks = userItemRepository.findByUserIdAndTypeAndTaskStatusOrderByDueDateAsc(requestingUserId, UserItemType.TASK, statusFilter);
        } else {
            tasks = userItemRepository.findByUserIdAndTypeOrderByDueDateAsc(requestingUserId, UserItemType.TASK);
        }
        return tasks.stream().map(taskNoteMapper::toTaskResponse).toList();
    }

    @Override
    public TaskResponse getTaskDetail(Integer requestingUserId, Integer taskId) {
        UserItem item = userItemRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (item.getType() != UserItemType.TASK || !item.getUserId().equals(requestingUserId)) {
            log.warn("Task detail access denied — taskId: {}, requestingUserId: {}", taskId, requestingUserId);
            throw new RuntimeException("Access denied");
        }

        return taskNoteMapper.toTaskResponse(item);
    }

    // -------------------------------------------------------------------------
    // Notes
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public NoteResponse createNote(CreateNoteRequest request, Integer requestingUserId) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserItem saved = userItemRepository.save(taskNoteMapper.toNoteEntity(request, requestingUserId));
        log.info("Note created — id: {}, userId: {}", saved.getId(), requestingUserId);
        return taskNoteMapper.toNoteResponse(saved);
    }

    @Override
    @Transactional
    public NoteResponse updateNote(Integer requestingUserId, Integer noteId, UpdateNoteRequest request) {
        UserItem item = userItemRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (item.getType() != UserItemType.NOTE || !item.getUserId().equals(requestingUserId)) {
            log.warn("Note update denied — noteId: {}, requestingUserId: {}", noteId, requestingUserId);
            throw new RuntimeException("Access denied");
        }

        if (request.getTitle() != null) item.setTitle(request.getTitle());
        if (request.getDescription() != null) item.setDescription(request.getDescription());

        UserItem saved = userItemRepository.save(item);
        log.info("Note updated — id: {}, userId: {}", noteId, requestingUserId);
        return taskNoteMapper.toNoteResponse(saved);
    }

    @Override
    @Transactional
    public String deleteNote(Integer requestingUserId, Integer noteId) {
        UserItem item = userItemRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (item.getType() != UserItemType.NOTE || !item.getUserId().equals(requestingUserId)) {
            log.warn("Note delete denied — noteId: {}, requestingUserId: {}", noteId, requestingUserId);
            throw new RuntimeException("Access denied");
        }

        userItemRepository.delete(item);
        log.info("Note deleted — id: {}, userId: {}", noteId, requestingUserId);
        return "Note deleted successfully";
    }

    @Override
    public List<NoteResponse> getNotes(Integer requestingUserId) {
        return userItemRepository.findByUserIdAndTypeOrderByUpdatedAtDesc(requestingUserId, UserItemType.NOTE)
                .stream().map(taskNoteMapper::toNoteResponse).toList();
    }

    @Override
    public NoteResponse getNoteDetail(Integer requestingUserId, Integer noteId) {
        UserItem item = userItemRepository.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found"));

        if (item.getType() != UserItemType.NOTE || !item.getUserId().equals(requestingUserId)) {
            log.warn("Note detail access denied — noteId: {}, requestingUserId: {}", noteId, requestingUserId);
            throw new RuntimeException("Access denied");
        }

        return taskNoteMapper.toNoteResponse(item);
    }
}
