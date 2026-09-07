package com.codingedu.service;

import com.codingedu.entity.LessonCourse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 강의 본문(레슨·코드 예제)은 classpath:data/lesson-courses.json 에 두고,
 * 목록/진도용 메타데이터(title, icon, lessonCount 등)는 DB {@link LessonCourse}와 맞춥니다.
 */
@Service
public class LessonContentService {

    private static final Logger log = LoggerFactory.getLogger(LessonContentService.class);

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public LessonContentService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    /**
     * JSON 코스 맵에 DB의 title·icon을 덮어씌워 클라이언트(learn-detail)에 전달합니다.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildCoursesPayload(List<LessonCourse> dbCourses) {
        Map<String, Object> root = loadCoursesRoot();
        if (root.isEmpty()) {
            return root;
        }
        for (LessonCourse lc : dbCourses) {
            Object raw = root.get(lc.getLang());
            if (!(raw instanceof Map)) {
                continue;
            }
            Map<String, Object> course = (Map<String, Object>) raw;
            course.put("title", lc.getTitle());
            course.put("icon", lc.getIcon());
            Object lessonsObj = course.get("lessons");
            if (lessonsObj instanceof List<?> lessons) {
                int n = lessons.size();
                if (n != lc.getLessonCount()) {
                    log.warn("lesson count mismatch for lang={}: json lessons={} db lessonCount={}",
                            lc.getLang(), n, lc.getLessonCount());
                }
            }
        }
        return root;
    }

    /**
     * 강의 목록 카드에서 사용할 코스별 대표 유튜브 영상 ID를 반환합니다.
     */
    public Map<String, String> getCourseVideoIds() {
        Map<String, Object> root = loadCoursesRoot();
        if (root.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> videoIds = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            Object raw = entry.getValue();
            if (!(raw instanceof Map<?, ?> course)) {
                continue;
            }
            Object videoId = course.get("videoId");
            if (videoId instanceof String id && !id.isBlank()) {
                videoIds.put(entry.getKey(), id);
            }
        }
        return videoIds;
    }

    private Map<String, Object> loadCoursesRoot() {
        Resource resource = resourceLoader.getResource("classpath:data/lesson-courses.json");
        if (!resource.exists()) {
            log.error("classpath:data/lesson-courses.json not found");
            return Collections.emptyMap();
        }
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        } catch (IOException e) {
            log.error("Failed to load lesson-courses.json", e);
            return Collections.emptyMap();
        }
    }
}

