package com.codingedu.service;

import com.codingedu.entity.Comment;
import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 5000;

    private final CommentRepository commentRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.notificationService = notificationService;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public long countCommentsByPostId(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public Map<Long, Long> countCommentsByPostIds(Collection<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = postIds.stream()
                .distinct()
                .collect(Collectors.toMap(Function.identity(), id -> 0L));

        commentRepository.countGroupedByPostIds(postIds).forEach(row ->
                counts.put((Long) row[0], (Long) row[1]));

        return counts;
    }

    public record TopAnswerer(String nickname, long commentCount) {}

    public List<TopAnswerer> getTopAnswerersThisWeek(int limit) {
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        return commentRepository.findTopAnswerersThisWeek(weekStart).stream()
                .limit(limit)
                .map(row -> new TopAnswerer((String) row[0], (Long) row[1]))
                .toList();
    }

    @Transactional
    public Comment addComment(String content, Post post, User author) {
        Comment comment = new Comment();
        comment.setContent(requireContent(content));
        comment.setPost(post);
        comment.setAuthor(author);
        commentRepository.save(comment);
        notificationService.createCommentNotification(post, author);

        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        deleteComment(commentId, null, username);
    }

    @Transactional
    public void deleteComment(Long commentId, Long postId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment does not exist."));
        if (postId != null && !comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("Comment does not belong to the requested post.");
        }
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("No permission to delete this comment.");
        }
        commentRepository.delete(comment);
    }

    private String requireContent(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("content is required.");
        }
        if (trimmed.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("content is too long.");
        }
        return trimmed;
    }
}
