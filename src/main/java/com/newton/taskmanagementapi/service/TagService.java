package com.newton.taskmanagementapi.service;

import com.newton.taskmanagementapi.dto.TagDetailResponse;
import com.newton.taskmanagementapi.dto.TagResponse;
import com.newton.taskmanagementapi.dto.TaskResponse;
import com.newton.taskmanagementapi.exception.ResourceNotFoundException;
import com.newton.taskmanagementapi.model.Tag;
import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.TagRepository;
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
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public Set<Tag> getOrCreateTags(Set<String> tagNames, User user) {
        Set<Tag> tags = new HashSet<>();

        for (String tagName : tagNames) {
            String normalizedName = tagName.trim().toLowerCase();
            Tag tag = tagRepository.findByNameAndUser(normalizedName, user)
                    .orElseGet(() -> {
                        Tag newTag = Tag.builder()
                                .name(normalizedName)
                                .user(user)
                                .tasks(new HashSet<>())
                                .build();
                        return tagRepository.save(newTag);
                    });
            tags.add(tag);
        }

        return tags;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(User user) {
        List<Tag> tags = tagRepository.findByUserOrderByNameAsc(user);

        return tags.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TagDetailResponse getTagById(Long tagId, User user) {
        Tag tag = tagRepository.findByIdAndUserWithTasks(tagId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        return mapToDetailResponse(tag);
    }

    public TagResponse mapToResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .taskCount(tag.getTasks() != null ? tag.getTasks().size() : 0)
                .build();
    }

    private TagDetailResponse mapToDetailResponse(Tag tag) {
        Set<TaskResponse> taskResponses = tag.getTasks().stream()
                .map(task -> TaskResponse.builder()
                        .id(task.getId())
                        .title(task.getTitle())
                        .description(task.getDescription())
                        .dueDate(task.getDueDate())
                        .completed(task.getCompleted())
                        .googleEventId(task.getGoogleEventId())
                        .createdAt(task.getCreatedAt())
                        .updatedAt(task.getUpdatedAt())
                        .build())
                .collect(Collectors.toSet());

        return TagDetailResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .tasks(taskResponses)
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}
