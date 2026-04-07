package com.lmi.crm.service.crud;

import com.lmi.crm.dao.UserItemRepository;
import com.lmi.crm.dto.UserItemRequestDTO;
import com.lmi.crm.dto.UserItemResponseDTO;
import com.lmi.crm.entity.UserItem;
import com.lmi.crm.mapper.UserItemMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserItemServiceImpl implements UserItemService {

    @Autowired
    private UserItemRepository userItemRepository;

    @Autowired
    private UserItemMapper userItemMapper;

    @Override
    public UserItemResponseDTO createUserItem(UserItemRequestDTO request) {
        log.info("Creating new user item for user ID {}", request.getUserId());
        UserItem userItem = userItemMapper.toEntity(request);
        return userItemMapper.toDTO(userItemRepository.save(userItem));
    }

    @Override
    public UserItemResponseDTO getUserItemById(Integer id) {
        UserItem userItem = userItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("UserItem with ID {} not found", id);
                    return new EntityNotFoundException("UserItem not found with ID: " + id);
                });
        return userItemMapper.toDTO(userItem);
    }

    @Override
    public List<UserItemResponseDTO> getAllUserItems() {
        return userItemRepository.findAll()
                .stream()
                .map(userItemMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserItemResponseDTO updateUserItem(Integer id, UserItemRequestDTO request) {
        log.info("Updating user item with ID {}", id);
        UserItem userItem = userItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("UserItem with ID {} not found", id);
                    return new EntityNotFoundException("UserItem not found with ID: " + id);
                });
        userItemMapper.updateEntity(userItem, request);
        return userItemMapper.toDTO(userItemRepository.save(userItem));
    }

    @Override
    public void deleteUserItem(Integer id) {
        log.info("Deleting user item with ID {}", id);
        UserItem userItem = userItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("UserItem with ID {} not found", id);
                    return new EntityNotFoundException("UserItem not found with ID: " + id);
                });
        userItemRepository.delete(userItem);
    }
}
