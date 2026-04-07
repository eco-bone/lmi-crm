package com.lmi.crm.service.crud;

import com.lmi.crm.dto.UserRequestDTO;
import com.lmi.crm.dto.UserResponseDTO;

import java.util.List;

/**
 * Service interface defining the contract for User CRUD operations.
 *
 * In Spring Boot, it is standard practice to define business logic behind an interface
 * and provide a concrete implementation separately (UserServiceImpl). This allows for
 * loose coupling — the controller depends on this interface, not the implementation,
 * making it easy to swap or mock the implementation (e.g. in tests).
 *
 * DTOs (Data Transfer Objects) are used instead of raw entities to control exactly
 * what data enters and leaves the API, keeping the internal model decoupled from
 * the API contract.
 */
public interface UserService {

    /**
     * Creates a new user from the provided request data.
     * The user is persisted to the database and the saved record is returned.
     *
     * @param request the incoming data for the new user
     * @return the created user as a response DTO
     */
    UserResponseDTO createUser(UserRequestDTO request);

    /**
     * Fetches a single user by their primary key.
     * Throws EntityNotFoundException if no user exists with the given ID,
     * which will surface as a 404 response once an exception handler is wired up.
     *
     * @param id the user's primary key
     * @return the matching user as a response DTO
     */
    UserResponseDTO getUserById(Integer id);

    /**
     * Fetches all users in the system.
     * Returns an empty list if no users exist — never returns null.
     *
     * @return list of all users as response DTOs
     */
    List<UserResponseDTO> getAllUsers();

    /**
     * Updates an existing user's details using the provided request data.
     * Fetches the existing record first to ensure it exists, applies changes,
     * then persists and returns the updated record.
     *
     * @param id      the ID of the user to update
     * @param request the new data to apply
     * @return the updated user as a response DTO
     */
    UserResponseDTO updateUser(Integer id, UserRequestDTO request);

    /**
     * Soft deletes a user by setting their status to INACTIVE.
     * The record is retained in the database — it is simply no longer considered active.
     * Throws EntityNotFoundException if the user does not exist.
     *
     * @param id the ID of the user to deactivate
     */
    void deleteUser(Integer id);
}
