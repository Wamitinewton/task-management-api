package com.newton.taskmanagementapi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.authorized-redirect-uris[0]}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        UserPrincipal userPrincipal;

        if (authentication.getPrincipal() instanceof CustomOidcUser) {
            CustomOidcUser customOidcUser = (CustomOidcUser) authentication.getPrincipal();
            userPrincipal = customOidcUser.getUserPrincipal();
        } else if (authentication.getPrincipal() instanceof UserPrincipal) {
            userPrincipal = (UserPrincipal) authentication.getPrincipal();
        } else {
            log.error("Unexpected principal type: {}", authentication.getPrincipal().getClass());
            throw new IllegalStateException("Unexpected principal type");
        }

        String accessToken = jwtUtil.generateToken(userPrincipal.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(userPrincipal.getEmail());

        return UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();
    }
}