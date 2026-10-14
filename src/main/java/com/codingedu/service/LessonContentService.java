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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 레슨 points의 "영상 길이 m:ss" 값을 합산한 코스별 총 영상 길이(분)를 반환합니다.
     * 영상 길이가 빠진 레슨이 하나라도 있는 코스는 추정치를 만들지 않도록 제외합니다.
     */
    public Map<String, Integer> getCourseDurationMinutes() {
        Map<String, Object> root = loadCoursesRoot();
        Map<String, Integer> minutes = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> course)
                    || !(course.get("lessons") instanceof List<?> lessons) || lessons.isEmpty()) {
                continue;
            }
            int totalSeconds = 0;
            boolean complete = true;
            for (Object lesson : lessons) {
                int seconds = lesson instanceof Map<?, ?> l ? parseVideoSeconds(l.get("points")) : -1;
                if (seconds < 0) {
                    complete = false;
                    break;
                }
                totalSeconds += seconds;
            }
            if (complete) {
                minutes.put(entry.getKey(), Math.round(totalSeconds / 60f));
            }
        }
        return minutes;
    }

    private static final Pattern VIDEO_LENGTH = Pattern.compile("영상 길이 (?:(\\d+):)?(\\d+):(\\d{2})");

    private int parseVideoSeconds(Object points) {
        if (!(points instanceof List<?> list)) {
            return -1;
        }
        for (Object point : list) {
            if (point instanceof String text) {
                Matcher m = VIDEO_LENGTH.matcher(text);
                if (m.find()) {
                    int hours = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
                    return hours * 3600 + Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3));
                }
            }
        }
        return -1;
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

