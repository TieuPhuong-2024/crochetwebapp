package org.crochet.service;

import org.crochet.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);

    RefreshToken createRefreshToken(String username, boolean rememberMe);

    Optional<RefreshToken> findActiveToken(String userId);

    RefreshToken ensureActiveRefreshToken(String userId);

    RefreshToken rotateRefreshToken(String token);

    void revokeRefreshToken(String token);

    void revokeAllRefreshTokens(String userId);

    void deleteExpiredRefreshTokens();
}
