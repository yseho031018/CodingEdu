package com.codingedu.service;

import com.codingedu.entity.LessonCourse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LessonContentServiceTest {

    @Test
    void loadsCoursesFromClasspathJson() {
        LessonContentService svc = new LessonContentService(new ObjectMapper(), new DefaultResourceLoader());
        Map<String, Object> payload = svc.buildCoursesPayload(List.of());
        assertThat(payload).containsKey("html");
        @SuppressWarnings("unchecked")
        Map<String, Object> html = (Map<String, Object>) payload.get("html");
        assertThat(html).containsKeys("title", "icon", "lessons");
    }

    @Test
    void mergesTitleAndIconFromDb() {
        LessonContentService svc = new LessonContentService(new ObjectMapper(), new DefaultResourceLoader());
        LessonCourse lc = new LessonCourse();
        lc.setLang("html");
        lc.setTitle("Merged Title");
        lc.setIcon("📘");
        lc.setLessonCount(9);

        Map<String, Object> payload = svc.buildCoursesPayload(List.of(lc));
        @SuppressWarnings("unchecked")
        Map<String, Object> html = (Map<String, Object>) payload.get("html");
        assertThat(html.get("title")).isEqualTo("Merged Title");
        assertThat(html.get("icon")).isEqualTo("📘");
    }

    @Test
    void lessonJsonHasExpectedLangKeysAndLessonArrays() {
        LessonContentService svc = new LessonContentService(new ObjectMapper(), new DefaultResourceLoader());
        Map<String, Object> payload = svc.buildCoursesPayload(List.of());
        Set<String> expected = Set.of(
                "html", "css", "javascript", "typescript", "java", "kotlin",
                "c", "cpp", "swift", "python"
        );
        assertThat(payload.keySet()).containsAll(expected);
        for (String lang : expected) {
            assertThat(payload.get(lang)).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> course = (Map<String, Object>) payload.get(lang);
            assertThat(course).containsKey("lessons");
            assertThat(course.get("lessons")).isInstanceOf(List.class);
            assertThat((List<?>) course.get("lessons")).isNotEmpty();
        }
    }
}
