package com.newton.taskmanagementapi.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    @EqualsAndHashCode.Include
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(name = "google_access_token")
    private String googleAccessToken;

    @Column(name = "google_refresh_token")
    private String googleRefreshToken;

    @Column(name = "token_expiry_date")
    private LocalDateTime tokenExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.GOOGLE;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private Set<Task> tasks = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AuthProvider {
        GOOGLE,
    }
}