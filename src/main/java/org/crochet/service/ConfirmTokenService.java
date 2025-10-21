package org.crochet.service;

import org.crochet.model.ConfirmationToken;
import org.crochet.model.User;

import java.time.Instant;

public interface ConfirmTokenService {
    ConfirmationToken createOrUpdate(User user);

    void updateConfirmedAt(String token, Instant dateTime);

    ConfirmationToken getToken(String token);

    void deleteExpiredOrConfirmedTokens();
}
