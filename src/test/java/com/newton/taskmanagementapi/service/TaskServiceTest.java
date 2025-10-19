package com.newton.taskmanagementapi.service;

import com.newton.taskmanagementapi.dto.CreateTaskRequest;
import com.newton.taskmanagementapi.dto.TaskResponse;
import com.newton.taskmanagementapi.dto.UpdateTaskRequest;
import com.newton.taskmanagementapi.exception.ResourceNotFoundException;
import com.newton.taskmanagementapi.model.Tag;
import com.newton.taskmanagementapi.model.Task;
import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagService tagService;

    @Mock
    private GoogleCalenderService googleCalenderService;

    @InjectMocks
    private TaskService taskService;

    private User testUser;
    private Task testTask;
    private Tag testTag;


    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .authProvider(User.AuthProvider.GOOGLE)
                .build();

        testTag = Tag.builder()
                .id(1L)
                .name("work")
                .user(testUser)
                .tasks(new HashSet<>())
                .build();

        testTask = Task.builder()
                .id(1L)
                .title("Test Task")
                .description("Test Description")
                .dueDate(LocalDateTime.now().plusDays(1))
                .completed(false)
                .user(testUser)
                .tags(new HashSet<>(Set.of(testTag)))
                .build();
    }

    @Test
    void createTask_Success() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .dueDate(LocalDateTime.now().plusDays(1))
                .completed(false)
                .tags(Set.of("work"))
                .build();

        when(tagService.getOrCreateTags(any(), any())).thenReturn(Set.of(testTag));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(googleCalenderService.createCalendarEvent(any(), any())).thenReturn("event-123");

        TaskResponse response = taskService.createTask(request, testUser);

        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
        verify(taskRepository, times(2)).save(any(Task.class));
        verify(googleCalenderService).createCalendarEvent(any(), any());
    }

    @Test
    void createTask_WithoutDueDate_NoCalendarSync() {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("New Task")
                .description("New Description")
                .completed(false)
                .build();

        Task taskWithoutDueDate = Task.builder()
                .id(1L)
                .title("New Task")
                .description("New Description")
                .completed(false)
                .user(testUser)
                .tags(new HashSet<>())
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(taskWithoutDueDate);

        TaskResponse response = taskService.createTask(request, testUser);

        assertNotNull(response);
        verify(googleCalenderService, never()).createCalendarEvent(any(), any());
    }

    @Test
    void getAllTasks_Success() {
        when(taskRepository.findByUserOrderByDueDateAsc(testUser))
                .thenReturn(Collections.singletonList(testTask));

        List<TaskResponse> responses = taskService.getAllTasks(testUser, null, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Test Task", responses.get(0).getTitle());
    }

    @Test
    void getAllTasks_FilterByCompleted() {
        when(taskRepository.findByUserAndCompletedOrderByDueDateAsc(testUser, false))
                .thenReturn(Collections.singletonList(testTask));

        List<TaskResponse> responses = taskService.getAllTasks(testUser, false, null);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(taskRepository).findByUserAndCompletedOrderByDueDateAsc(testUser, false);
    }

    @Test
    void getTaskById_Success() {
        when(taskRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testTask));

        TaskResponse response = taskService.getTaskById(1L, testUser);

        assertNotNull(response);
        assertEquals("Test Task", response.getTitle());
    }

    @Test
    void getTaskById_NotFound() {
        when(taskRepository.findByIdAndUser(999L, testUser))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.getTaskById(999L, testUser));
    }

    @Test
    void updateTask_Success() {
        UpdateTaskRequest request = UpdateTaskRequest.builder()
                .title("Updated Task")
                .completed(true)
                .build();

        when(taskRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        TaskResponse response = taskService.updateTask(1L, request, testUser);

        assertNotNull(response);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void deleteTask_Success() {
        testTask.setGoogleEventId("event-123");

        when(taskRepository.findByIdAndUser(1L, testUser))
                .thenReturn(Optional.of(testTask));
        doNothing().when(googleCalenderService).deleteCalendarEvent(any(), any());
        doNothing().when(taskRepository).delete(any(Task.class));

        taskService.deleteTask(1L, testUser);

        verify(googleCalenderService).deleteCalendarEvent("event-123", testUser);
        verify(taskRepository).delete(testTask);
    }

    @Test
    void deleteTask_NotFound() {
        when(taskRepository.findByIdAndUser(999L, testUser))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> taskService.deleteTask(999L, testUser));
    }
}
