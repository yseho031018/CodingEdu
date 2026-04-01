package com.codingedu.repository;

import com.codingedu.entity.Notification;
import com.codingedu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByReceiverOrderByCreatedAtDesc(User receiver);
    long countByReceiverAndIsReadFalse(User receiver);
}
