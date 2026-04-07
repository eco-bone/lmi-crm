package com.lmi.crm.service.crud;

import com.lmi.crm.dao.UserRepository;
import com.lmi.crm.dto.UserRequestDTO;
import com.lmi.crm.dto.UserResponseDTO;
import com.lmi.crm.entity.User;
import com.lmi.crm.enums.UserStatus;
import com.lmi.crm.mapper.UserMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Concrete implementation of UserService, containing the actual business logic
 * for User CRUD operations.
 *
 * @Service — marks this class as a Spring-managed service bean. Spring automatically
 * detects it during component scanning and registers it in the application context,
 * making it available for dependency injection wherever UserService is required.
 *
 * @Slf4j — Lombok annotation that auto-generates a static 'log' field using SLF4J,
 * giving us a logger without any boilerplate. Logs are written at different levels
 * (info, error) depending on the significance of the event.
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    /**
     * @Autowired instructs Spring to inject the appropriate bean automatically.
     * UserRepository extends JpaRepository, which Spring Data JPA implements at
     * runtime — we never write the SQL or JDBC code ourselves for standard operations.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * UserMapper handles conversion between the raw entity (User) and DTOs.
     * Keeping this logic in a dedicated mapper class prevents the service from
     * becoming cluttered with field-mapping code.
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * Converts the incoming request DTO to a User entity, persists it,
     * and returns the saved record as a response DTO.
     *
     * userRepository.save() performs an INSERT when the entity has no ID set,
     * and returns the saved entity with the auto-generated ID populated.
     */
    @Override
    public UserResponseDTO createUser(UserRequestDTO request) {
        log.info("Creating new user with email {}", request.getEmail());
        User user = userMapper.toEntity(request);
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Fetches a user by ID using JPA's Optional-based findById.
     * orElseThrow() unwraps the Optional — if the record exists it is returned,
     * otherwise the provided exception is thrown. EntityNotFoundException will
     * be mapped to an HTTP 404 once a global exception handler is added.
     */
    @Override
    public UserResponseDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", id);
                    return new EntityNotFoundException("User not found with ID: " + id);
                });
        return userMapper.toDTO(user);
    }

    /**
     * Retrieves all user records and maps them to response DTOs using a stream.
     * Stream + map() is the idiomatic Java way to transform a collection —
     * equivalent to Array.map() in JavaScript or LINQ Select() in C#.
     */
    @Override
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Fetches the existing user first to confirm it exists, then delegates
     * field updates to the mapper's updateEntity() method to keep mapping
     * logic out of the service. The modified entity is then re-saved.
     *
     * save() on an entity that already has an ID performs an UPDATE, not an INSERT.
     * Hibernate detects this because the entity is in a managed state after findById().
     */
    @Override
    public UserResponseDTO updateUser(Integer id, UserRequestDTO request) {
        log.info("Updating user with ID {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", id);
                    return new EntityNotFoundException("User not found with ID: " + id);
                });

        userMapper.updateEntity(user, request);
        return userMapper.toDTO(userRepository.save(user));
    }

    /**
     * Soft deletes a user by setting their status to INACTIVE rather than removing
     * the record from the database. This preserves historical data and any foreign key
     * references that point to this user (e.g. audit logs, prospects created by this user).
     *
     * The record is then re-saved, which triggers an UPDATE on the status column only.
     */
    @Override
    public void deleteUser(Integer id) {
        log.info("Soft deleting user with ID {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User with ID {} not found", id);
                    return new EntityNotFoundException("User not found with ID: " + id);
                });
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }
}
