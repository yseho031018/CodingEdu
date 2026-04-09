package com.codingedu.controller;

import com.codingedu.repository.LessonNoteRepository;
import com.codingedu.service.LessonContentService;
import com.codingedu.service.LessonService;
import com.codingedu.service.NotificationService;
import com.codingedu.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = LearnController.class)
@Import(GlobalModelAdvice.class)
@AutoConfigureMockMvc(addFilters = false)
class LearnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LessonService lessonService;
    @MockBean
    private LessonContentService lessonContentService;
    @MockBean
    private UserService userService;
    @MockBean
    private LessonNoteRepository lessonNoteRepository;
    @MockBean
    private NotificationService notificationService;

    @Test
    void learnListOk() throws Exception {
        when(lessonService.getAllCourses()).thenReturn(List.of());
        mockMvc.perform(get("/learn"))
                .andExpect(status().isOk())
                .andExpect(view().name("learn"));
    }
}
