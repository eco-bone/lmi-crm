package com.lmi.crm.dto;

import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.enums.UserItemType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserItemRequestDTO {
    private Integer userId;
    private UserItemType type;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private TaskStatus taskStatus;
}
