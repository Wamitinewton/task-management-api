package com.newton.taskmanagementapi.security;

import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = new OAuth2UserInfo(oAuth2User.getAttributes());

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2UserInfo, userRequest);
        } else {
            user = registerNewUser(oAuth2UserInfo, userRequest);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserInfo oAuth2UserInfo, OAuth2UserRequest userRequest) {
        User user = User.builder()
                .name(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .googleId(oAuth2UserInfo.getId())
                .profilePictureUrl(oAuth2UserInfo.getImageUrl())
                .authProvider(User.AuthProvider.GOOGLE)
                .googleAccessToken(userRequest.getAccessToken().getTokenValue())
                .googleRefreshToken(extractRefreshToken(userRequest))
                .tokenExpiryDate(LocalDateTime.now().plusSeconds(
                        userRequest.getAccessToken().getExpiresAt() != null ?
                                userRequest.getAccessToken().getExpiresAt().getEpochSecond() -
                                        userRequest.getAccessToken().getIssuedAt().getEpochSecond() : 3600
                ))
                .build();

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo, OAuth2UserRequest userRequest) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setProfilePictureUrl(oAuth2UserInfo.getImageUrl());
        existingUser.setGoogleAccessToken(userRequest.getAccessToken().getTokenValue());

        String refreshToken = extractRefreshToken(userRequest);
        if (refreshToken != null) {
            existingUser.setGoogleRefreshToken(refreshToken);
        }

        existingUser.setTokenExpiryDate(LocalDateTime.now().plusSeconds(
                userRequest.getAccessToken().getExpiresAt() != null ?
                        userRequest.getAccessToken().getExpiresAt().getEpochSecond() -
                                userRequest.getAccessToken().getIssuedAt().getEpochSecond() : 3600
        ));

        return userRepository.save(existingUser);
    }

    private String extractRefreshToken(OAuth2UserRequest userRequest) {
      return userRequest.getAdditionalParameters().get("refresh_token") != null
              ? userRequest.getAdditionalParameters().get("refresh_token").toString()
              : null;
    }
}