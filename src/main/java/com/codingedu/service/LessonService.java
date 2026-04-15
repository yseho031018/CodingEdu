package com.codingedu.service;

import com.codingedu.entity.LearningProgress;
import com.codingedu.entity.LessonCourse;
import com.codingedu.entity.User;
import com.codingedu.repository.LearningProgressRepository;
import com.codingedu.repository.LessonCourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class LessonService {

    private final LessonCourseRepository courseRepository;
    private final LearningProgressRepository progressRepository;

    public LessonService(LessonCourseRepository courseRepository,
                         LearningProgressRepository progressRepository) {
        this.courseRepository = courseRepository;
        this.progressRepository = progressRepository;
    }

    public List<LessonCourse> getAllCourses() {
        return courseRepository.findAllByOrderByIdAsc();
    }

    public LessonCourse getCourseByLang(String lang) {
        return courseRepository.findByLang(lang).orElse(null);
    }

    // 언어별 완료한 강의 인덱스 집합 반환
    public Set<Integer> getCompletedIndices(User user, String lang) {
        return progressRepository.findByUserAndLang(user, lang)
                .stream()
                .map(LearningProgress::getLessonIdx)
                .collect(Collectors.toSet());
    }

    // 전체 언어별 완료 강의 수 반환 (learn.html 진도 표시용)
    public Map<String, Integer> getCompletedCountMap(User user) {
        return getAllCourses().stream().collect(Collectors.toMap(
                LessonCourse::getLang,
                c -> progressRepository.countByUserAndLang(user, c.getLang())
        ));
    }

    @Transactional
    public void markComplete(User user, String lang, int lessonIdx) {
        if (!progressRepository.existsByUserAndLangAndLessonIdx(user, lang, lessonIdx)) {
            LearningProgress progress = new LearningProgress();
            progress.setUser(user);
            progress.setLang(lang);
            progress.setLessonIdx(lessonIdx);
            progressRepository.save(progress);
        }
    }

    public int getTotalCompletedCount(User user) {
        return progressRepository.countByUser(user);
    }
}
