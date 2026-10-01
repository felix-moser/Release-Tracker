package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.client.JiraClient;
import com.felixcasa.backendjava.exception.TemplateNotFoundException;
import com.felixcasa.backendjava.repository.JiraRepository;
import com.felixcasa.backendjava.response.TicketTemplateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JiraServiceTest {

    @Mock
    private JiraRepository jiraRepository;

    @Mock
    private SystemService systemService;

    @Mock
    private JiraClient jiraClient;

    @InjectMocks
    private JiraService jiraService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jiraService, "jiraClient", jiraClient);
    }


    @Test
    void authHeaderShouldReturnBase64EncodedString() {
        when(systemService.getSecret("JIRA_EMAIL")).thenReturn("test@test.com");
        when(systemService.getSecret("JIRA_API_TOKEN")).thenReturn("token");

        String authHeader = jiraService.authHeader();

        String expectedToken = Base64.getEncoder().encodeToString("test@test.com:token".getBytes(StandardCharsets.UTF_8));
        assertThat(authHeader).isEqualTo("Basic " + expectedToken);
    }


    @Test
    void toAdfShouldReturnEmptyDocWhenTextIsNull() {
        Map<String, Object> adf = jiraService.toAdf(null);

        assertThat(adf).containsEntry("type", "doc");
        List<Map<String, Object>> content = (List<Map<String, Object>>) adf.get("content");
        assertThat(content).hasSize(1);
        List<Map<String, String>> innerContent = (List<Map<String, String>>) content.get(0).get("content");
        assertThat(innerContent.get(0)).containsEntry("text", "");
    }

    @Test
    void toAdfShouldReturnEmptyDocWhenTextIsEmpty() {
        Map<String, Object> adf = jiraService.toAdf("");

        assertThat(adf).containsEntry("type", "doc");
        List<Map<String, Object>> content = (List<Map<String, Object>>) adf.get("content");
        assertThat(content).hasSize(1);
        List<Map<String, String>> innerContent = (List<Map<String, String>>) content.get(0).get("content");
        assertThat(innerContent.get(0)).containsEntry("text", "");
    }

    @Test
    void toAdfShouldReturnDocWithParagraphsWhenTextIsProvided() {
        Map<String, Object> adf = jiraService.toAdf("Line 1\nLine 2");

        List<Map<String, Object>> content = (List<Map<String, Object>>) adf.get("content");
        assertThat(content).hasSize(2);
        
        List<Map<String, String>> innerContent1 = (List<Map<String, String>>) content.get(0).get("content");
        assertThat(innerContent1.get(0)).containsEntry("text", "Line 1");
        
        List<Map<String, String>> innerContent2 = (List<Map<String, String>>) content.get(1).get("content");
        assertThat(innerContent2.get(0)).containsEntry("text", "Line 2");
    }


    @Test
    void buildIssuePayloadShouldCreateFullPayloadWithPlaceholdersReplaced() {
        TicketTemplateResponse template = new TicketTemplateResponse(
                "template1", "PROJ", "Bug", "Summary {repo} {tag}", "Desc {url}",
                "assignee123", "High", "3", "bug,backend,", "Done"
        );

        Map<String, Object> payload = jiraService.buildIssuePayload(template, "my-repo", "v1.0", "http://url");

        Map<String, Object> fields = (Map<String, Object>) payload.get("fields");
        
        assertThat(fields.get("summary")).isEqualTo("Summary my-repo v1.0");
        assertThat(fields).containsEntry("project", Map.of("key", "PROJ"));
        assertThat(fields).containsEntry("issuetype", Map.of("name", "Bug"));
        assertThat(fields).containsEntry("assignee", Map.of("id", "assignee123"));
        assertThat(fields).containsEntry("priority", Map.of("name", "High"));
        assertThat((String) fields.get("duedate")).isEqualTo(LocalDate.now().plusDays(3).toString());
        assertThat(fields).containsEntry("labels", List.of("bug", "backend"));
    }

    @Test
    void buildIssuePayloadShouldHandleNullValuesAndNonNumericDueDate() {
        TicketTemplateResponse template = new TicketTemplateResponse(
                "template1", "PROJ", "Bug", "Summary {repo}", null,
                "", "", "2024-12-31", "", ""
        );

        Map<String, Object> payload = jiraService.buildIssuePayload(template, null, null, null);

        Map<String, Object> fields = (Map<String, Object>) payload.get("fields");
        
        assertThat(fields.get("summary")).isEqualTo("Summary ");
        assertThat(fields).doesNotContainKey("assignee");
        assertThat(fields).doesNotContainKey("priority");
        assertThat(fields).doesNotContainKey("labels");
        assertThat(fields.get("duedate")).isEqualTo("2024-12-31");
    }


    @Test
    void transitionIssueShouldExecuteSuccessfully() {
        when(systemService.getSecret("JIRA_EMAIL")).thenReturn("test@test.com");
        when(systemService.getSecret("JIRA_API_TOKEN")).thenReturn("token");

        List<Map<String, Object>> transitions = List.of(
                Map.of("id", "31", "name", "Done")
        );
        when(jiraClient.getTransitions(eq("PROJ-123"), anyString(), eq("https://jira.com"))).thenReturn(transitions);

        jiraService.transitionIssue("PROJ-123", "Done", "https://jira.com");

        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("test@test.com:token".getBytes(StandardCharsets.UTF_8));
        verify(jiraClient).executeTransition("PROJ-123", "31", expectedAuth, "https://jira.com");
    }

    @Test
    void transitionIssueShouldThrowRuntimeExceptionWhenTransitionsAreNull() {
        when(systemService.getSecret("JIRA_EMAIL")).thenReturn("test@test.com");
        when(systemService.getSecret("JIRA_API_TOKEN")).thenReturn("token");
        when(jiraClient.getTransitions(eq("PROJ-123"), anyString(), eq("https://jira.com"))).thenReturn(null);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jiraService.transitionIssue("PROJ-123", "Done", "https://jira.com")
        );

        assertEquals("Failed to fetch transitions for PROJ-123", exception.getMessage());
    }

    @Test
    void transitionIssueShouldThrowRuntimeExceptionWhenTransitionNotAvailable() {
        when(systemService.getSecret("JIRA_EMAIL")).thenReturn("test@test.com");
        when(systemService.getSecret("JIRA_API_TOKEN")).thenReturn("token");

        List<Map<String, Object>> transitions = List.of(
                Map.of("id", "11", "name", "In Progress")
        );
        when(jiraClient.getTransitions(eq("PROJ-123"), anyString(), eq("https://jira.com"))).thenReturn(transitions);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> jiraService.transitionIssue("PROJ-123", "Done", "https://jira.com")
        );

        assertEquals("Transition 'Done' not available.", exception.getMessage());
    }


    @Test
    void createJiraTicketShouldCreateIssueAndTransitionSuccessfully() {
        TicketTemplateResponse template = new TicketTemplateResponse(
                "template1", "PROJ", "Bug", "Summary {repo}", "Desc {tag}",
                "assignee123", "High", "3", "bug,backend", "Done"
        );

        when(jiraRepository.getTicketTemplate("template1")).thenReturn(Optional.of(template));
        when(systemService.getSecret("JIRA_EMAIL")).thenReturn("test@test.com");
        when(systemService.getSecret("JIRA_API_TOKEN")).thenReturn("token");
        when(systemService.getSecret("JIRA_URL")).thenReturn("https://jira.com/");

        when(jiraClient.createIssue(anyMap(), anyString(), anyString())).thenReturn("PROJ-123");

        List<Map<String, Object>> transitions = List.of(
                Map.of("id", "31", "name", "Done")
        );
        when(jiraClient.getTransitions(eq("PROJ-123"), anyString(), eq("https://jira.com"))).thenReturn(transitions);

        String result = jiraService.createJiraTicket("template1", "repo1", "v1", "http://url");

        assertThat(result).isEqualTo("PROJ-123");

        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString("test@test.com:token".getBytes(StandardCharsets.UTF_8));
        verify(jiraClient).executeTransition("PROJ-123", "31", expectedAuth, "https://jira.com");
    }

    @Test
    void createJiraTicketShouldThrowTemplateNotFoundExceptionWhenTemplateIsNotFound() {
        when(jiraRepository.getTicketTemplate("unknown")).thenReturn(Optional.empty());

        TemplateNotFoundException exception = assertThrows(
                TemplateNotFoundException.class,
                () -> jiraService.createJiraTicket("unknown", null, null, null)
        );

        assertEquals("Template not found", exception.getMessage());
    }
}
