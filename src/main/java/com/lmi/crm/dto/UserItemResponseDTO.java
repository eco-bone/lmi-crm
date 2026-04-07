package com.lmi.crm.dto;

import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.enums.UserItemType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserItemResponseDTO {
    private Integer id;
    private Integer userId;
    private UserItemType type;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private TaskStatus taskStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
