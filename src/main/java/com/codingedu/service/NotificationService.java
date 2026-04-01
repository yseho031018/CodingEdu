package com.codingedu.service;

import com.codingedu.entity.Notification;
import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void createCommentNotification(Post post, User commenter) {
        User postAuthor = post.getAuthor();
        // 자기 자신의 글에 댓글 달면 알림 없음
        if (postAuthor.getId().equals(commenter.getId())) return;

        Notification notification = new Notification();
        notification.setReceiver(postAuthor);
        notification.setType("COMMENT");
        notification.setMessage("'" + post.getTitle() + "' 게시글에 " + commenter.getNickname() + "님이 댓글을 달았습니다.");
        notification.setPostId(post.getId());
        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(User user) {
        return notificationRepository.findByReceiverOrderByCreatedAtDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByReceiverAndIsReadFalse(user);
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> list = notificationRepository.findByReceiverOrderByCreatedAtDesc(user);
        list.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(list);
    }
}
