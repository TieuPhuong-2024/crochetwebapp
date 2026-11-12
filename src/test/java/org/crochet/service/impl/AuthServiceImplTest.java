package org.crochet.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.crochet.exception.TokenException;
import org.crochet.exception.UnauthorizedException;
import org.crochet.model.RefreshToken;
import org.crochet.model.User;
import org.crochet.payload.response.UserResponse;
import org.crochet.service.ConfirmTokenService;
import org.crochet.service.EmailSender;
import org.crochet.service.JwtTokenService;
import org.crochet.service.PasswordResetTokenService;
import org.crochet.service.RefreshTokenService;
import org.crochet.service.TokenBlacklistService;
import org.crochet.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ConfirmTokenService confirmTokenService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;
    @Mock
    private UserService userService;
    @Mock
    private EmailSender emailSender;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refreshToken_returnsRotatedRefreshTokenAndNewAccessToken() {
        var user = User.builder().id("user-1").build();
        var rotated = RefreshToken.builder().token("new-refresh").user(user).build();
        when(refreshTokenService.rotateRefreshToken("old-refresh")).thenReturn(rotated);
        when(jwtTokenService.generateToken("user-1")).thenReturn("new-access");

        var response = authService.refreshToken("old-refresh");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void logout_blacklistsAccessTokenAndRevokesRefreshToken() {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");

        authService.logout(request, "refresh-token");

        verify(tokenBlacklistService).addTokenToBlacklist("access-token");
        verify(refreshTokenService).revokeRefreshToken("refresh-token");
    }

    @Test
    void logoutAll_revokesAllUserRefreshTokens() {
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(jwtTokenService.extractUsername("access-token")).thenReturn("user-1");

        authService.logoutAll(request);

        verify(refreshTokenService).revokeAllRefreshTokens("user-1");
        verify(tokenBlacklistService).addTokenToBlacklist("access-token");
    }

    @Test
    void logoutAll_withoutAccessToken_isRejected() {
        assertThatThrownBy(() -> authService.logoutAll(request))
                .isInstanceOf(UnauthorizedException.class);

        verify(refreshTokenService, never()).revokeAllRefreshTokens(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getCurrentUser_withoutActiveRefreshToken_doesNotCreateToken() {
        var user = new UserResponse();
        user.setId("user-1");
        var securityContext = mock(SecurityContext.class);
        var authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(jwtTokenService.extractUsername("access-token")).thenReturn("user-1");
        when(userService.getDetail("user-1")).thenReturn(user);
        when(refreshTokenService.findActiveToken("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(request))
                .isInstanceOf(TokenException.class);

        verify(refreshTokenService, never()).ensureActiveRefreshToken("user-1");
    }
}
