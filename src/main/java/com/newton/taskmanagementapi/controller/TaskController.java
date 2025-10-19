package com.newton.taskmanagementapi.controller;

import com.newton.taskmanagementapi.dto.CreateTaskRequest;
import com.newton.taskmanagementapi.dto.TaskResponse;
import com.newton.taskmanagementapi.dto.UpdateTaskRequest;
import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.UserRepository;
import com.newton.taskmanagementapi.security.UserPrincipal;
import com.newton.taskmanagementapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create task", description = "Create a new task with optional tags")
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        TaskResponse response = taskService.createTask(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all tasks", description = "Get all tasks with optional filters")
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) List<String> tags,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        List<TaskResponse> tasks = taskService.getAllTasks(user, completed, tags);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID", description = "Get a specific task by its ID")
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        TaskResponse task = taskService.getTaskById(id, user);
        return ResponseEntity.ok(task);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task", description = "Update an existing task")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        TaskResponse task = taskService.updateTask(id, request, user);
        return ResponseEntity.ok(task);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task", description = "Delete a task and its associated calendar event")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    private User getUserFromPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}