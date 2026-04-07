package com.lmi.crm.mapper;

import com.lmi.crm.dto.UserItemRequestDTO;
import com.lmi.crm.dto.UserItemResponseDTO;
import com.lmi.crm.entity.UserItem;
import org.springframework.stereotype.Component;

@Component
public class UserItemMapper {

    public UserItem toEntity(UserItemRequestDTO dto) {
        UserItem userItem = new UserItem();
        userItem.setUserId(dto.getUserId());
        userItem.setType(dto.getType());
        userItem.setTitle(dto.getTitle());
        userItem.setDescription(dto.getDescription());
        userItem.setDueDate(dto.getDueDate());
        userItem.setTaskStatus(dto.getTaskStatus());
        return userItem;
    }

    public void updateEntity(UserItem userItem, UserItemRequestDTO dto) {
        userItem.setUserId(dto.getUserId());
        userItem.setType(dto.getType());
        userItem.setTitle(dto.getTitle());
        userItem.setDescription(dto.getDescription());
        userItem.setDueDate(dto.getDueDate());
        userItem.setTaskStatus(dto.getTaskStatus());
    }

    public UserItemResponseDTO toDTO(UserItem userItem) {
        UserItemResponseDTO dto = new UserItemResponseDTO();
        dto.setId(userItem.getId());
        dto.setUserId(userItem.getUserId());
        dto.setType(userItem.getType());
        dto.setTitle(userItem.getTitle());
        dto.setDescription(userItem.getDescription());
        dto.setDueDate(userItem.getDueDate());
        dto.setTaskStatus(userItem.getTaskStatus());
        dto.setCreatedAt(userItem.getCreatedAt());
        dto.setUpdatedAt(userItem.getUpdatedAt());
        return dto;
    }
}
