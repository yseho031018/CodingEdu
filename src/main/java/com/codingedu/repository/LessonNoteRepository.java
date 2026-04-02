package com.codingedu.repository;

import com.codingedu.entity.LessonNote;
import com.codingedu.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonNoteRepository extends JpaRepository<LessonNote, Long> {
    Optional<LessonNote> findByUserAndLangAndLessonIdx(User user, String lang, int lessonIdx);
    List<LessonNote> findByUserAndLang(User user, String lang);
}
