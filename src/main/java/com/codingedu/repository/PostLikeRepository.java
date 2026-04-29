package com.codingedu.repository;

import com.codingedu.entity.Post;
import com.codingedu.entity.PostLike;
import com.codingedu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserAndPost(User user, Post post);
    boolean existsByUserAndPost(User user, Post post);
    int countByPost(Post post);
    int countByPostAndReactionType(Post post, String reactionType);
}
