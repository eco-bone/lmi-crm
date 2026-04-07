package com.lmi.crm.service.crud;

import com.lmi.crm.dto.UserItemRequestDTO;
import com.lmi.crm.dto.UserItemResponseDTO;

import java.util.List;

public interface UserItemService {
    UserItemResponseDTO createUserItem(UserItemRequestDTO request);
    UserItemResponseDTO getUserItemById(Integer id);
    List<UserItemResponseDTO> getAllUserItems();
    UserItemResponseDTO updateUserItem(Integer id, UserItemRequestDTO request);
    void deleteUserItem(Integer id);
}
