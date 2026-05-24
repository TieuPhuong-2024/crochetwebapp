package org.crochet.repository;

import org.crochet.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    Optional<Subscription> findByUserIdAndStatus(String userId, org.crochet.enums.SubscriptionStatus status);

    java.util.List<Subscription> findByStatusAndEndDateBefore(org.crochet.enums.SubscriptionStatus status, java.time.LocalDateTime date);
}
