package org.crochet.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.crochet.properties.AppProperties;
import org.crochet.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtTokenServiceImplTest {

    @Test
    void generateToken_usesConfiguredFifteenMinuteExpiration() {
        var appProperties = new AppProperties();
        appProperties.getAuth().setTokenSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        appProperties.getAuth().setTokenExpirationMs(900_000L);
        var jwtTokenService = new JwtTokenServiceImpl(appProperties, mock(TokenBlacklistService.class));

        var token = jwtTokenService.generateToken("user-1");

        var key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(appProperties.getAuth().getTokenSecret()));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        var expirationMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(expirationMs).isBetween(899_000L, 901_000L);
    }
}
