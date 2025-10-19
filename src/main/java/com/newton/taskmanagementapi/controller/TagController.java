package com.newton.taskmanagementapi.controller;

import com.newton.taskmanagementapi.dto.TagDetailResponse;
import com.newton.taskmanagementapi.dto.TagResponse;
import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.UserRepository;
import com.newton.taskmanagementapi.security.UserPrincipal;
import com.newton.taskmanagementapi.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Tags", description = "Tag management endpoints")
public class TagController {

    private final TagService tagService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get all tags", description = "Get all tags with task counts")
    public ResponseEntity<List<TagResponse>> getAllTags(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        List<TagResponse> tags = tagService.getAllTags(user);
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tag details", description = "Get tag details with all associated tasks")
    public ResponseEntity<TagDetailResponse> getTagById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        User user = getUserFromPrincipal(userPrincipal);
        TagDetailResponse tag = tagService.getTagById(id, user);
        return ResponseEntity.ok(tag);
    }

    private User getUserFromPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
