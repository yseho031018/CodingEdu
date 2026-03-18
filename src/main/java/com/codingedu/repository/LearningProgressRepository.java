package com.codingedu.repository;

import com.codingedu.entity.LearningProgress;
import com.codingedu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {
    List<LearningProgress> findByUserAndLang(User user, String lang);
    boolean existsByUserAndLangAndLessonIdx(User user, String lang, int lessonIdx);
    int countByUserAndLang(User user, String lang);
}
