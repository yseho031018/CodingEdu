package com.codingedu.repository;

import com.codingedu.entity.Post;
import com.codingedu.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Post> findTop5ByAuthorOrderByCreatedAtDesc(User author);
    int countByAuthor(User author);
}
