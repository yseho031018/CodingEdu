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

    private final CommentRepository commentRepository;
    private final PostService postService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, PostService postService, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postService = postService;
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
        comment.setContent(content);
        comment.setPost(post);
        comment.setAuthor(author);
        commentRepository.save(comment);
        postService.incrementCommentCount(post);
        notificationService.createCommentNotification(post, author);

        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
        if (!comment.getAuthor().getUsername().equals(username)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }
        Post post = comment.getPost();
        commentRepository.delete(comment);
        postService.decrementCommentCount(post);
    }
}
