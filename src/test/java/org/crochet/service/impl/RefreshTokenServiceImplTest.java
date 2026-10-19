package org.crochet.service.impl;

import org.crochet.model.RefreshToken;
import org.crochet.model.User;
import org.crochet.properties.AppProperties;
import org.crochet.repository.RefreshTokenRepo;
import org.crochet.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepo refreshTokenRepo;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppProperties appProps;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    void ensureActiveRefreshToken_returnsExisting_whenPresent() {
        var token = RefreshToken.builder().token("rt-1").build();
        when(refreshTokenRepo.findFirstByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(token));

        var result = refreshTokenService.ensureActiveRefreshToken("user-1");

        assertThat(result).isSameAs(token);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void findActiveToken_returnsEmpty_whenNone() {
        when(refreshTokenRepo.findFirstByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                eq("user-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        assertThat(refreshTokenService.findActiveToken("user-1")).isEmpty();
    }
}
