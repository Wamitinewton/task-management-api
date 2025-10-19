package com.newton.taskmanagementapi.service;

import com.newton.taskmanagementapi.dto.CreateTaskRequest;
import com.newton.taskmanagementapi.dto.TaskResponse;
import com.newton.taskmanagementapi.dto.UpdateTaskRequest;
import com.newton.taskmanagementapi.exception.ResourceNotFoundException;
import com.newton.taskmanagementapi.model.Tag;
import com.newton.taskmanagementapi.model.Task;
import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TagService tagService;
    private final GoogleCalenderService googleCalendarService;

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, User user) {
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .completed(request.getCompleted() != null ? request.getCompleted() : false)
                .user(user)
                .tags(new HashSet<>())
                .build();

        // Handle tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<Tag> tags = tagService.getOrCreateTags(request.getTags(), user);
            tags.forEach(task::addTag);
        }

        task = taskRepository.save(task);

        // Sync with Google Calendar if due date is set
        if (task.getDueDate() != null) {
            try {
                String eventId = googleCalendarService.createCalendarEvent(task, user);
                task.setGoogleEventId(eventId);
                task = taskRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to create calendar event for task {}", task.getId(), e);
            }
        }

        return mapToResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks(User user, Boolean completed, List<String> tags) {
        List<Task> tasks;

        if (tags != null && !tags.isEmpty()) {
            tasks = taskRepository.findByUserAndTagNames(user, tags);
        } else if (completed != null) {
            tasks = taskRepository.findByUserAndCompletedOrderByDueDateAsc(user, completed);
        } else {
            tasks = taskRepository.findByUserOrderByDueDateAsc(user);
        }

        return tasks.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long taskId, User user) {
        Task task = taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, User user) {
        Task task = taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        boolean calendarUpdateNeeded = false;

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
            calendarUpdateNeeded = true;
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
            calendarUpdateNeeded = true;
        }

        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
            calendarUpdateNeeded = true;
        }

        if (request.getCompleted() != null) {
            task.setCompleted(request.getCompleted());
            calendarUpdateNeeded = true;
        }

        // Handle tags update
        if (request.getTags() != null) {
            task.clearTags();
            if (!request.getTags().isEmpty()) {
                Set<Tag> tags = tagService.getOrCreateTags(request.getTags(), user);
                tags.forEach(task::addTag);
            }
        }

        task = taskRepository.save(task);

        // Update Google Calendar event
        if (calendarUpdateNeeded && task.getGoogleEventId() != null) {
            try {
                googleCalendarService.updateCalendarEvent(task, user);
            } catch (Exception e) {
                log.error("Failed to update calendar event for task {}", task.getId(), e);
            }
        } else if (task.getDueDate() != null && task.getGoogleEventId() == null) {
            // Create calendar event if due date is set but no event exists
            try {
                String eventId = googleCalendarService.createCalendarEvent(task, user);
                task.setGoogleEventId(eventId);
                task = taskRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to create calendar event for task {}", task.getId(), e);
            }
        }

        return mapToResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId, User user) {
        Task task = taskRepository.findByIdAndUser(taskId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Delete from Google Calendar
        if (task.getGoogleEventId() != null) {
            try {
                googleCalendarService.deleteCalendarEvent(task.getGoogleEventId(), user);
            } catch (Exception e) {
                log.error("Failed to delete calendar event for task {}", task.getId(), e);
            }
        }

        task.clearTags();
        taskRepository.delete(task);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .completed(task.getCompleted())
                .googleEventId(task.getGoogleEventId())
                .tags(task.getTags().stream()
                        .map(tagService::mapToResponse)
                        .collect(Collectors.toSet()))
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
