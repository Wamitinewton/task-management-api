package com.newton.taskmanagementapi.repository;

import com.newton.taskmanagementapi.model.Task;
import com.newton.taskmanagementapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserOrderByDueDateAsc(User user);

    List<Task> findByUserAndCompletedOrderByDueDateAsc(User user, Boolean completed);

    @Query("SELECT t FROM Task t JOIN t.tags tag WHERE t.user = :user AND tag.name IN :tagNames")
    List<Task> findByUserAndTagNames(@Param("user") User user, @Param("tagNames") List<String> tagNames);

    Optional<Task> findByIdAndUser(Long id, User user);

    Optional<Task> findByGoogleEventId(String googleEventId);
}