package com.codingedu.service;

import com.codingedu.entity.LessonCourse;
import com.codingedu.service.LearningRoadmapService.RoadmapStep;
import com.codingedu.service.LearningRoadmapService.RoadmapTrack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningRoadmapServiceTest {

    private final LessonService lessonService = mock(LessonService.class);
    private final LessonContentService lessonContentService = mock(LessonContentService.class);
    private final LearningRoadmapService service = new LearningRoadmapService(lessonService, lessonContentService);

    LearningRoadmapServiceTest() {
        List<LessonCourse> courses = Stream.of("html", "css", "javascript", "react", "nextjs", "git", "php",
                        "java", "mysql", "sqljoin", "python")
                .map(lang -> {
                    LessonCourse c = new LessonCourse();
                    c.setLang(lang);
                    c.setLevel("beginner");
                    c.setLessonCount(10);
                    return c;
                }).toList();
        when(lessonService.getAllCourses()).thenReturn(courses);
        when(lessonContentService.getCourseDurationMinutes()).thenReturn(Map.of("html", 209, "git", 45));
    }

    @Test
    void anonymousUserStartsAtFirstStepOfEveryTrack() {
        List<RoadmapTrack> tracks = service.buildTracks(null);

        assertThat(tracks).extracting(RoadmapTrack::key)
                .containsExactly("web", "frontend", "backend", "database", "python");
        RoadmapTrack web = tracks.get(0);
        assertThat(web.steps()).extracting(RoadmapStep::status)
                .containsExactly("current", "next", "later", "upcoming");
        assertThat(web.courseCount()).isEqualTo(3);
        assertThat(web.steps().get(0).duration()).isEqualTo("약 3시간 30분");
        assertThat(web.steps().get(1).duration()).isNull();
        assertThat(web.steps().get(1).prerequisite()).isEqualTo("선수 강의: HTML & Internet");
        assertThat(service.initialTrack(tracks)).isEqualTo("web");
    }

    @Test
    void currentStepFollowsCompletedCoursesAndOpensTrackInProgress() {
        List<RoadmapTrack> tracks = service.buildTracks(Map.of("mysql", 100, "sqljoin", 40));

        RoadmapTrack database = tracks.get(3);
        assertThat(database.steps()).extracting(RoadmapStep::status).containsExactly("done", "current");
        assertThat(database.progress()).isEqualTo(70);
        assertThat(tracks.get(0).steps().get(0).status()).isEqualTo("current");
        assertThat(service.initialTrack(tracks)).isEqualTo("database");
    }
}
