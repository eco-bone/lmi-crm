package com.lmi.crm.dao;

import com.lmi.crm.entity.UserItem;
import com.lmi.crm.enums.TaskStatus;
import com.lmi.crm.enums.UserItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserItemRepository extends JpaRepository<UserItem, Integer> {

    List<UserItem> findByUserIdAndTypeOrderByDueDateAsc(Integer userId, UserItemType type);

    List<UserItem> findByUserIdAndTypeAndTaskStatusOrderByDueDateAsc(Integer userId, UserItemType type, TaskStatus taskStatus);

    List<UserItem> findByUserIdAndTypeOrderByUpdatedAtDesc(Integer userId, UserItemType type);

    List<UserItem> findByTypeAndTaskStatusAndDueDateBetween(UserItemType type, TaskStatus taskStatus, LocalDateTime start, LocalDateTime end);
}
