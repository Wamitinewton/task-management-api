package com.newton.taskmanagementapi.security;


import com.newton.taskmanagementapi.model.User;
import com.newton.taskmanagementapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        return processOidcUser(userRequest, oidcUser);
    }

    private OidcUser processOidcUser(OidcUserRequest userRequest, OidcUser oidcUser) {
        OAuth2UserInfo oAuth2UserInfo = new OAuth2UserInfo(oidcUser.getAttributes());

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            user = updateExistingUser(user, oAuth2UserInfo, userRequest);
        } else {
            user = registerNewUser(oAuth2UserInfo, userRequest);
        }

        return new CustomOidcUser(UserPrincipal.create(user, oidcUser.getAttributes()), oidcUser);
    }

    private User registerNewUser(OAuth2UserInfo oAuth2UserInfo, OidcUserRequest userRequest) {
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

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo, OidcUserRequest userRequest) {
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

    private String extractRefreshToken(OidcUserRequest userRequest) {
        return userRequest.getAdditionalParameters().get("refresh_token") != null
                ? userRequest.getAdditionalParameters().get("refresh_token").toString()
                : null;
    }
}
