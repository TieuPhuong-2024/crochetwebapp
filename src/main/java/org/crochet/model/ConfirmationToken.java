package org.crochet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

import io.hypersistence.utils.hibernate.id.Tsid;

@Getter
@Setter
@Entity
@Table(name = "confirmation_token")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmationToken {
    @Id
    @Tsid
    @Column(name = "id", nullable = false, unique = true, length = 50)
    private String id;

    @Column(name = "token",
            unique = true,
            nullable = false,
            updatable = false)
    private String token;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;
}
