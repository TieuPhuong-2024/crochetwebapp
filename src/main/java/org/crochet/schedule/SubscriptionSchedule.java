package org.crochet.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.crochet.enums.RoleType;
import org.crochet.enums.SubscriptionStatus;
import org.crochet.model.Subscription;
import org.crochet.model.User;
import org.crochet.repository.SubscriptionRepository;
import org.crochet.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionSchedule {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void expireSubscriptions() {
        log.info("Running scheduled task to expire subscriptions...");
        List<Subscription> expiredSubscriptions = subscriptionRepository.findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        
        for (Subscription sub : expiredSubscriptions) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            
            User user = sub.getUser();
            if (user.getRole() == RoleType.PREMIUM_USER) {
                user.setRole(RoleType.USER);
                userRepository.save(user);
                log.info("User {} subscription expired. Reverted to USER role.", user.getEmail());
            }
        }
        
        if (!expiredSubscriptions.isEmpty()) {
            subscriptionRepository.saveAll(expiredSubscriptions);
            log.info("Expired {} subscriptions.", expiredSubscriptions.size());
        }
    }
}
