package com.felixcasa.backendjava.controller;

import com.felixcasa.backendjava.request.SecretsRequest;
import com.felixcasa.backendjava.response.SecretsResponse;
import com.felixcasa.backendjava.service.SystemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@WebMvcTest(SystemController.class)
public class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SystemService systemService;

    @Test
    public void shouldReturnOnboardedTrueAndStatusOk() throws Exception{
        when(systemService.checkOnboarded()).thenReturn(Map.of("onboarded", Boolean.TRUE));

        mockMvc.perform(get("/api/onboarded"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"onboarded\":true}"));
    }

    @Test
    public void shouldReturnSecretsAndStatusOk() throws Exception{
        when(systemService.getSecrets()).thenReturn(new SecretsResponse("https://example.atlassian.net","example@example.com",true, true));

        mockMvc.perform(get("/api/secrets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jira_url", is("https://example.atlassian.net")))
                .andExpect(jsonPath("$.jira_email", is("example@example.com")))
                .andExpect(jsonPath("$.jira_api_token", is(true)))
                .andExpect(jsonPath("$.github_api_key", is(true)));
    }

    @Test
    public void shouldReturnSavedMessageAndStatusOkay() throws Exception {
        when(systemService.saveSecrets(new SecretsRequest("https://example.atlassian.net","example@example.com","API-Token-Jira","API-Key-Github"))).thenReturn(Map.of("message","Secrets saved successfully"));

        mockMvc.perform(post("/api/secrets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SecretsRequest("https://example.atlassian.net","example@example.com","API-Token-Jira","API-Key-Github"))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"Secrets saved successfully\"}"));
    }
}
