package com.newton.taskmanagementapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newton.taskmanagementapi.dto.CreateTaskRequest;
import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.UserRepository;
import com.newton.taskmanagementapi.security.JwtUtil;
import com.newton.taskmanagementapi.service.GoogleCalenderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private GoogleCalenderService googleCalenderService;

    private String authToken;
    private User testUser;

    @TestConfiguration
    static class MockConfig {
        @Bean
        public GoogleCalenderService googleCalenderService() {
            return org.mockito.Mockito.mock(GoogleCalenderService.class);
        }
    }

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("integration@test.com")
                .name("Integration Test User")
                .googleId("google-123")
                .authProvider(User.AuthProvider.GOOGLE)
                .build();
        testUser = userRepository.save(testUser);

        authToken = jwtUtil.generateToken(testUser.getEmail());

        when(googleCalenderService.createCalendarEvent(any(), any())).thenReturn("mock-event-id");
    }

    @Test
    void createTask_Success() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("Integration Test Task")
                .description("Test Description")
                .dueDate(LocalDateTime.now().plusDays(1))
                .completed(false)
                .tags(Set.of("work", "urgent"))
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.tags", hasSize(2)));
    }

    @Test
    void createTask_ValidationError() throws Exception {
        CreateTaskRequest request = CreateTaskRequest.builder()
                .title("") // Empty title should fail
                .build();

        mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTasks_Success() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllTasks_WithCompletedFilter() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .param("completed", "false")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllTasks_Unauthorized() throws Exception {
        // Spring Security redirects unauthorized requests (302) instead of returning 403
        // This is the expected behavior for session-based authentication
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isFound()); // 302 redirect to login
    }

    @Test
    void getTaskById_NotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/999")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTask_Success() throws Exception {
        // First create a task
        CreateTaskRequest createRequest = CreateTaskRequest.builder()
                .title("Task to Update")
                .description("Original Description")
                .completed(false)
                .build();

        String response = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

        // Now update it
        mockMvc.perform(put("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void deleteTask_Success() throws Exception {
        CreateTaskRequest createRequest = CreateTaskRequest.builder()
                .title("Task to Delete")
                .build();

        String response = mockMvc.perform(post("/api/tasks")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long taskId = objectMapper.readTree(response).get("id").asLong();

       mockMvc.perform(delete("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + taskId)
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isNotFound());
    }
}