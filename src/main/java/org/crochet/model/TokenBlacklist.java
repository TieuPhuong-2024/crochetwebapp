package org.crochet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "token_blacklist")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist {
    @Id
    @Tsid
    @Column(name = "id", nullable = false, unique = true, length = 50)
    private String id;
    
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "blacklisted_at")
    private Instant blacklistedAt;
}
