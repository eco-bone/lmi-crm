package com.lmi.crm.mapper;

import com.lmi.crm.dto.request.CreateNoteRequest;
import com.lmi.crm.dto.request.CreateTaskRequest;
import com.lmi.crm.dto.response.NoteResponse;
import com.lmi.crm.dto.response.TaskResponse;
import com.lmi.crm.entity.UserItem;
import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.enums.UserItemType;
import org.springframework.stereotype.Component;

@Component
public class TaskNoteMapper {

    public UserItem toTaskEntity(CreateTaskRequest request, Integer userId) {
        return UserItem.builder()
                .userId(userId)
                .type(UserItemType.TASK)
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .taskStatus(TaskStatus.PENDING)
                .build();
    }

    public UserItem toNoteEntity(CreateNoteRequest request, Integer userId) {
        return UserItem.builder()
                .userId(userId)
                .type(UserItemType.NOTE)
                .title(request.getTitle())
                .description(request.getDescription())
                .build();
    }

    public TaskResponse toTaskResponse(UserItem item) {
        return TaskResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .title(item.getTitle())
                .description(item.getDescription())
                .dueDate(item.getDueDate())
                .status(item.getTaskStatus())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public NoteResponse toNoteResponse(UserItem item) {
        return NoteResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .title(item.getTitle())
                .description(item.getDescription())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
