package com.lmi.crm.service;

import com.lmi.crm.dao.UserItemRepository;
import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.request.CreateNoteRequest;
import com.lmi.crm.dto.request.CreateTaskRequest;
import com.lmi.crm.dto.request.UpdateNoteRequest;
import com.lmi.crm.dto.request.UpdateTaskRequest;
import com.lmi.crm.dto.response.NoteResponse;
import com.lmi.crm.dto.response.NotesPageResponse;
import com.lmi.crm.dto.response.NotesSummaryResponse;
import com.lmi.crm.dto.response.TaskResponse;
import com.lmi.crm.dto.response.TasksPageResponse;
import com.lmi.crm.dto.response.TasksSummaryResponse;
import com.lmi.crm.entity.UserItem;
import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.enums.UserItemType;
import com.lmi.crm.mapper.TaskNoteMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    public Object getTasks(Integer requestingUserId, boolean getAll, TaskStatus statusFilter, int page, int limit) {
        userRepository.findById(requestingUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<UserItem> allTasks = userItemRepository.findByUserIdAndTypeOrderByDueDateAsc(requestingUserId, UserItemType.TASK);

        long totalCount = allTasks.size();
        long pendingCount = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.PENDING).count();
        long completedCount = allTasks.stream().filter(t -> t.getTaskStatus() == TaskStatus.COMPLETED).count();

        log.info("getTasks — requestingUserId: {}, getAll: {}, statusFilter: {}, page: {}, limit: {}, totalCount: {}",
                requestingUserId, getAll, statusFilter, page, limit, totalCount);

        if (getAll) {
            List<TaskResponse> allResponses = allTasks.stream().map(taskNoteMapper::toTaskResponse).toList();
            int end = Math.min(limit, allResponses.size());
            Page<TaskResponse> firstPage = new PageImpl<>(
                    end > 0 ? allResponses.subList(0, end) : List.of(),
                    PageRequest.of(0, limit),
                    allResponses.size());

            log.info("getTasks — getAll mode — requestingUserId: {}, totalCount: {}, pendingCount: {}, completedCount: {}",
                    requestingUserId, totalCount, pendingCount, completedCount);

            return TasksSummaryResponse.builder()
                    .totalCount(totalCount)
                    .pendingCount(pendingCount)
                    .completedCount(completedCount)
                    .firstPage(firstPage)
                    .build();
        } else {
            List<UserItem> filteredTasks = statusFilter != null
                    ? allTasks.stream().filter(t -> t.getTaskStatus() == statusFilter).toList()
                    : allTasks;

            List<TaskResponse> filteredResponses = filteredTasks.stream().map(taskNoteMapper::toTaskResponse).toList();

            int start = page * limit;
            int end = Math.min(start + limit, filteredResponses.size());
            List<TaskResponse> pageContent = start < filteredResponses.size()
                    ? filteredResponses.subList(start, end)
                    : List.of();
            Page<TaskResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), filteredResponses.size());

            log.info("getTasks — paginated mode — requestingUserId: {}, totalCount: {}, filteredTotal: {}, page: {}, limit: {}",
                    requestingUserId, totalCount, filteredResponses.size(), page, limit);

            return TasksPageResponse.builder()
                    .totalCount(totalCount)
                    .pendingCount(pendingCount)
                    .completedCount(completedCount)
                    .tasks(pageResult)
                    .build();
        }
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
    public Object getNotes(Integer requestingUserId, boolean getAll, int page, int limit) {
        List<UserItem> allNotes = userItemRepository.findByUserIdAndTypeOrderByUpdatedAtDesc(requestingUserId, UserItemType.NOTE);
        long totalCount = allNotes.size();

        log.info("getNotes — requestingUserId: {}, getAll: {}, page: {}, limit: {}, totalCount: {}",
                requestingUserId, getAll, page, limit, totalCount);

        List<NoteResponse> allResponses = allNotes.stream().map(taskNoteMapper::toNoteResponse).toList();

        if (getAll) {
            int end = Math.min(limit, allResponses.size());
            Page<NoteResponse> firstPage = new PageImpl<>(
                    end > 0 ? allResponses.subList(0, end) : List.of(),
                    PageRequest.of(0, limit),
                    allResponses.size());

            log.info("getNotes — getAll mode — requestingUserId: {}, totalCount: {}", requestingUserId, totalCount);

            return NotesSummaryResponse.builder()
                    .totalCount(totalCount)
                    .firstPage(firstPage)
                    .build();
        } else {
            int start = page * limit;
            int end = Math.min(start + limit, allResponses.size());
            List<NoteResponse> pageContent = start < allResponses.size()
                    ? allResponses.subList(start, end)
                    : List.of();
            Page<NoteResponse> pageResult = new PageImpl<>(pageContent, PageRequest.of(page, limit), allResponses.size());

            log.info("getNotes — paginated mode — requestingUserId: {}, totalCount: {}, page: {}, limit: {}",
                    requestingUserId, totalCount, page, limit);

            return NotesPageResponse.builder()
                    .totalCount(totalCount)
                    .notes(pageResult)
                    .build();
        }
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
