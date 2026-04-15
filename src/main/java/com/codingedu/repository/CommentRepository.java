package com.codingedu.repository;

import com.codingedu.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    @Query("SELECT c.post.id, COUNT(c) FROM Comment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countGroupedByPostIds(@Param("postIds") Collection<Long> postIds);

    @Query("SELECT c.author.nickname, COUNT(c) FROM Comment c WHERE c.createdAt >= :since GROUP BY c.author.id, c.author.nickname ORDER BY COUNT(c) DESC")
    List<Object[]> findTopAnswerersThisWeek(@Param("since") LocalDateTime since);
}
