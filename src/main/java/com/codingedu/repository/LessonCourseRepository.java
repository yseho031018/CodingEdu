package com.codingedu.repository;

import com.codingedu.entity.LessonCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonCourseRepository extends JpaRepository<LessonCourse, Long> {
    Optional<LessonCourse> findByLang(String lang);
    List<LessonCourse> findAllByOrderByIdAsc();
}
