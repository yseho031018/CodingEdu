package com.codingedu.service;

import com.codingedu.entity.Comment;
import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import com.codingedu.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
