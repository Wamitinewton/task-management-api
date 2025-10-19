package com.newton.taskmanagementapi.repository;

import com.newton.taskmanagementapi.model.Tag;
import com.newton.taskmanagementapi.model.User;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByNameAndUser(String name, User user);

    List<Tag> findByUserOrderByNameAsc(User user);

    Optional<Tag> findByIdAndUser(Long id, User user);

    @Query("SELECT t FROM Tag t LEFT JOIN FETCH t.tasks WHERE t.id = :id AND t.user = :user")
    Optional<Tag> findByIdAndUserWithTasks(Long id, User user);

    Boolean existsByNameAndUser(String name, User user);
}
