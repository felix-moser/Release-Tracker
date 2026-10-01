package com.felixcasa.backendjava.client;

import com.felixcasa.backendjava.response.JiraTickerTransitionResponse;
import com.felixcasa.backendjava.response.JiraTicketResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class JiraClient {

    private final RestClient restClient;

    public JiraClient() {
        this.restClient = RestClient.builder()
                .defaultHeader("Content-Type","application/json")
                .build();
    }

    public String createIssue(Map<String, Object> payload, String auth, String url){
        var request = restClient.post().uri(url + "/rest/api/3/issue");

        JiraTicketResponse response = request
                .header("Authorization", auth)
                .body(payload)
                .retrieve()
                .body(JiraTicketResponse.class);

        return response != null ? response.key() : null;
    }

    public List<Map<String, Object>> getTransitions(String issueKey, String auth, String url){
        var request = restClient.get().uri(url + "/rest/api/3/issue/" + issueKey + "/transitions");

        JiraTickerTransitionResponse response = request
                .header("Authorization", auth)
                .retrieve()
                .body(JiraTickerTransitionResponse.class);
        return response != null ? response.transitions() : null;
    }

    public void executeTransition(String issueKey, String transitionId, String auth, String url){
        var request = restClient.post().uri(url + "/rest/api/3/issue/" + issueKey + "/transitions");

        request.header("Authorization", auth)
                .body(Map.of("transition", Map.of("id", transitionId)))
                .retrieve()
                .toBodilessEntity();
    }
}
