package org.crochet.service.impl;

import org.crochet.enums.ResultCode;
import org.crochet.exception.ResourceNotFoundException;
import org.crochet.exception.TokenException;
import org.crochet.model.RefreshToken;
import org.crochet.properties.AppProperties;
import org.crochet.repository.RefreshTokenRepo;
import org.crochet.repository.UserRepository;
import org.crochet.service.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepo refreshTokenRepo;
    private final UserRepository userRepository;
    private final AppProperties appProps;

    /**
     * Constructor for RefreshTokenServiceImpl
     *
     * @param refreshTokenRepo RefreshTokenRepo
     * @param userRepository    UserRepository
     * @param appProps         AppProperties
     */
    public RefreshTokenServiceImpl(RefreshTokenRepo refreshTokenRepo,
                                   UserRepository userRepository,
                                   AppProperties appProps) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.userRepository = userRepository;
        this.appProps = appProps;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findActiveToken(String userId) {
        return refreshTokenRepo.findFirstByUser_IdAndRevokedFalseAndExpiresAtAfterOrderByExpiresAtDesc(
                userId,
                LocalDateTime.now());
    }

    @Override
    @Transactional
    public RefreshToken ensureActiveRefreshToken(String userId) {
        return findActiveToken(userId).orElseGet(() -> createRefreshToken(userId));
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        return createRefreshToken(username, false);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username, boolean rememberMe) {
        var user = userRepository.findById(username)
                .orElseThrow(() -> new ResourceNotFoundException(ResultCode.MSG_USER_NOT_FOUND_WITH_EMAIL.message(),
                        ResultCode.MSG_USER_NOT_FOUND_WITH_EMAIL.code()));
        LocalDateTime now = LocalDateTime.now();
        long expirationMs = rememberMe
                ? appProps.getAuth().getRefreshTokenRememberMeExpirationMs()
                : appProps.getAuth().getRefreshTokenExpirationMs();
        var expiryDate = now.plus(expirationMs, ChronoUnit.MILLIS);
        var refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiresAt(expiryDate)
                .user(user)
                .build();
        revokeAllRefreshTokens(user.getId());
        return refreshTokenRepo.save(refreshToken);
    }

    @Override
    @Transactional(noRollbackFor = TokenException.class)
    public RefreshToken rotateRefreshToken(String token) {
        var currentToken = refreshTokenRepo.findByTokenForUpdate(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ResultCode.REFRESH_TOKEN_NOT_IN_DB.message(),
                        ResultCode.REFRESH_TOKEN_NOT_IN_DB.code()));
        var user = currentToken.getUser();

        if (currentToken.isRevoked()) {
            revokeAllRefreshTokens(user.getId());
            throw new TokenException(ResultCode.MSG_REFRESH_TOKEN_REUSED.message(),
                    ResultCode.MSG_REFRESH_TOKEN_REUSED.code());
        }
        if (!currentToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            refreshTokenRepo.delete(currentToken);
            throw new TokenException(ResultCode.MSG_REFRESH_TOKEN_EXPIRED.message(),
                    ResultCode.MSG_REFRESH_TOKEN_EXPIRED.code());
        }

        currentToken.setRevoked(true);
        refreshTokenRepo.save(currentToken);
        var rotatedToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .expiresAt(currentToken.getExpiresAt())
                .user(user)
                .build();
        return refreshTokenRepo.save(rotatedToken);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepo.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepo.save(refreshToken);
        });
    }

    @Override
    @Transactional
    public void revokeAllRefreshTokens(String userId) {
        var validRefreshTokens = refreshTokenRepo.findAllValidRefreshTokenByUserId(userId);
        validRefreshTokens.forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepo.save(token);
        });
    }

    /**
     * Delete expired and revoked refresh tokens
     */
    @Override
    @Transactional
    public void deleteExpiredRefreshTokens() {
        var refreshTokens = refreshTokenRepo.findAll()
                .stream()
                .filter(RefreshToken::isRevoked)
                .filter(token -> token.getExpiresAt().isBefore(LocalDateTime.now()))
                .toList();
        refreshTokenRepo.deleteAll(refreshTokens);
    }
}
