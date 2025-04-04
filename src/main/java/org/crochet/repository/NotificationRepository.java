package org.crochet.repository;

import org.crochet.model.Notification;
import org.crochet.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    long countUnreadNotifications(@Param("user") User user);
    
    void deleteAllByUser(User user);
} 