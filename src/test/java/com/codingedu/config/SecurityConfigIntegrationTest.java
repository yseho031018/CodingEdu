package com.codingedu.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void termsAndLearnAndPrivacyPermitted() throws Exception {
        mockMvc.perform(get("/terms")).andExpect(status().isOk());
        mockMvc.perform(get("/privacy")).andExpect(status().isOk());
        mockMvc.perform(get("/learn")).andExpect(status().isOk());
    }

    @Test
    void loginPageRendersForAnonymous() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void registerPageRendersForAnonymous() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void adminRedirectsToLoginWhenAnonymous() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void staticResourcesPermitted() throws Exception {
        mockMvc.perform(get("/css/common.css")).andExpect(status().isOk());
    }

    @Test
    void quizSubPathsPermittedForAnonymous() throws Exception {
        mockMvc.perform(get("/quiz/practice")).andExpect(status().isOk());
    }
}

