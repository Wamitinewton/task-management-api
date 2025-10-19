package com.newton.taskmanagementapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagDetailResponse {

    private Long id;
    private String name;
    private Set<TaskResponse> tasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
