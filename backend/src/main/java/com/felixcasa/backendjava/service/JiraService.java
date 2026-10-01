package com.felixcasa.backendjava.service;

import com.felixcasa.backendjava.client.JiraClient;
import com.felixcasa.backendjava.exception.TemplateNotFoundException;
import com.felixcasa.backendjava.repository.JiraRepository;
import com.felixcasa.backendjava.repository.RepoRepository;
import com.felixcasa.backendjava.request.TemplateRequest;
import com.felixcasa.backendjava.request.TicketTemplateRequest;
import com.felixcasa.backendjava.response.TicketTemplateResponse;
import com.felixcasa.backendjava.response.RepositoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class JiraService {
    private final JiraRepository jiraRepository;
    private final SystemService systemService;
    private final RepoRepository repoRepository;
    private final JiraClient jiraClient;

    public JiraService(JiraRepository jiraRepository, SystemService systemService, RepoRepository repoRepository, JiraClient jiraClient) {
        this.jiraRepository = jiraRepository;
        this.systemService = systemService;
        this.repoRepository = repoRepository;
        this.jiraClient = jiraClient;
    }

    public String authHeader(){
        String email = systemService.getSecret("JIRA_EMAIL");
        String token = systemService.getSecret("JIRA_API_TOKEN");
        if (email == null || token == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jira credentials are not fully configured.");
        }
        return "Basic " + Base64.getEncoder().encodeToString(
                (email + ":" + token).getBytes(StandardCharsets.UTF_8));
    }

    public Map<String, Object> toAdf(String text) {
        if (text == null || text.isEmpty()) {
            return Map.of(
                    "version", 1,
                    "type", "doc",
                    "content", List.of(
                            Map.of(
                                    "type", "paragraph",
                                    "content", List.of(Map.of("type", "text", "text", ""))
                            )
                    )
            );
        }

        List<Map<String, Object>> paragraphs = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            paragraphs.add(Map.of(
                    "type", "paragraph",
                    "content", List.of(Map.of("type", "text", "text", line))
            ));
        }

        return Map.of(
                "version", 1,
                "type", "doc",
                "content", paragraphs
        );
    }

    public Map<String, Object> buildIssuePayload(TicketTemplateResponse template, String repo, String tag, String url) {
        String repoStr = repo == null ? "" : repo;
        String tagStr = tag == null ? "" : tag;
        String urlStr = url == null ? "" : url;

        String summary = template.summary()
                .replace("{repo}", repoStr)
                .replace("{tag}", tagStr)
                .replace("{url}", urlStr);

        String descriptionText = template.description() == null ? "" : template.description();
        descriptionText = descriptionText
                .replace("{repo}", repoStr)
                .replace("{tag}", tagStr)
                .replace("{url}", urlStr);

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", Map.of("key", template.projectKey()));
        fields.put("summary", summary);
        fields.put("issuetype", Map.of("name", template.issueType()));
        fields.put("description", toAdf(descriptionText));

        if (template.assigneeId() != null && !template.assigneeId().isEmpty()) {
            fields.put("assignee", Map.of("id", template.assigneeId()));
        }

        if (template.priority() != null && !template.priority().isEmpty()) {
            fields.put("priority", Map.of("name", template.priority()));
        }

        if (template.dueDate() != null && !template.dueDate().isEmpty()) {
            String dueVal = template.dueDate().trim();
            if (dueVal.matches("\\d+")) {
                int days = Integer.parseInt(dueVal);
                fields.put("duedate", LocalDate.now().plusDays(days).toString());
            } else {
                fields.put("duedate", dueVal);
            }
        }

        if (template.labels() != null && !template.labels().isEmpty()) {
            String[] labels = template.labels().split(",");
            List<String> labelList = new ArrayList<>();
            for (String l : labels) {
                if (!l.trim().isEmpty()) {
                    labelList.add(l.trim());
                }
            }
            if (!labelList.isEmpty()) {
                fields.put("labels", labelList);
            }
        }

        return Map.of("fields", fields);
    }

    public void transitionIssue(String issueKey, String transitionName, String url) {
        String auth = authHeader();
        List<Map<String, Object>> transitions = jiraClient.getTransitions(issueKey, auth, url);
        if (transitions == null) {
            throw new RuntimeException("Failed to fetch transitions for " + issueKey);
        }

        String targetId = null;
        for (Map<String, Object> t : transitions) {
            String name = (String) t.get("name");
            if (name != null && name.equalsIgnoreCase(transitionName)) {
                targetId = (String) t.get("id");
                break;
            }
        }

        if (targetId == null) {
            throw new RuntimeException("Transition '" + transitionName + "' not available.");
        }

        jiraClient.executeTransition(issueKey, targetId, auth, url);
    }

    public String createJiraTicket(String templateName, String repo, String tag, String url) {
        TicketTemplateResponse template = jiraRepository.getTicketTemplate(templateName).orElse(null);
        if (template == null) {
            throw new TemplateNotFoundException("Template not found");
        }

        Map<String, Object> payload = buildIssuePayload(template, repo, tag, url);
        String baseUrl = systemService.getSecret("JIRA_URL").replaceAll("/+$", "");
        String issueKey = jiraClient.createIssue(payload, authHeader(), baseUrl);

        if (template.transitionName() != null && !template.transitionName().isEmpty()) {
            transitionIssue(issueKey, template.transitionName(), baseUrl);
        }

        return issueKey;
    }

    public boolean addTemplate(TemplateRequest request) {
        TicketTemplateRequest ticketReq = new TicketTemplateRequest(
                request.name(), request.projectKey(), request.issueType(),
                request.summary(), request.description(), request.assigneeId(),
                request.priority(), request.dueDate(), request.labels(), request.transition()
        );
        return jiraRepository.addTicketTemplate(ticketReq);
    }

    public List<TicketTemplateResponse> listTemplates() {
        return jiraRepository.listTicketTemplates();
    }

    public TicketTemplateResponse getTemplate(String name) {
        return jiraRepository.getTicketTemplate(name).orElse(null);
    }

    public boolean updateTemplate(String name, TemplateRequest request) {
        TicketTemplateRequest ticketReq = new TicketTemplateRequest(
                request.name(), request.projectKey(), request.issueType(),
                request.summary(), request.description(), request.assigneeId(),
                request.priority(), request.dueDate(), request.labels(), request.transition()
        );
        return jiraRepository.updateTicketTemplate(name, ticketReq);
    }

    public boolean deleteTemplate(String name) {
        return jiraRepository.deleteTicketTemplate(name);
    }

    public String testTemplate(String name) {
        TicketTemplateResponse existing = jiraRepository.getTicketTemplate(name).orElse(null);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template '" + name + "' not found.");
        }

        String jiraUrl = systemService.getSecret("JIRA_URL");
        String jiraEmail = systemService.getSecret("JIRA_EMAIL");
        String jiraApiToken = systemService.getSecret("JIRA_API_TOKEN");

        if (jiraUrl == null || jiraEmail == null || jiraApiToken == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jira credentials are not fully configured.");
        }

        String repoName = "release-tracker";
        String tag = "v1.0.0";
        String url = "https://github.com/official/release-tracker";

        List<RepositoryResponse> repos = repoRepository.listRepos();
        if (repos != null && !repos.isEmpty()) {
            RepositoryResponse sample = repos.get(0);
            repoName = sample.repo();
            tag = sample.latestTag() != null ? sample.latestTag() : "v1.0.0";
            url = sample.repoUrl();
        }

        try {
            return createJiraTicket(name, repoName, tag, url);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create test ticket: " + e.getMessage());
        }
    }
}
