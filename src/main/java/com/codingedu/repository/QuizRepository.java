package com.codingedu.repository;

import com.codingedu.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByDifficultyOrderByCreatedAtAsc(String difficulty);
    List<Quiz> findAllByOrderByCreatedAtAsc();
    List<Quiz> findByTopicOrderByCreatedAtAsc(String topic);
    List<Quiz> findByDifficultyAndTopicOrderByCreatedAtAsc(String difficulty, String topic);
    boolean existsByTitle(String title);

    @Query("SELECT DISTINCT q.topic, q.icon FROM Quiz q ORDER BY q.topic")
    List<Object[]> findDistinctTopicsWithIcons();
}
