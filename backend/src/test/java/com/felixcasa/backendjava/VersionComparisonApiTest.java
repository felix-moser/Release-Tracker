package com.felixcasa.backendjava;

import com.felixcasa.backendjava.client.GithubClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VersionComparisonApiTest {
    static final String database = temporaryDatabase();
    static final ObjectMapper json = new ObjectMapper();
    static final HttpClient http = HttpClient.newHttpClient();
    static final AtomicReference<String> fetchedTag = new AtomicReference<>();
    static final CopyOnWriteArrayList<String> tickets = new CopyOnWriteArrayList<>();
    static final AtomicInteger jiraStatus = new AtomicInteger(201);
    static final HttpServer jira = jiraStub();

    @LocalServerPort int port;
    @Autowired JdbcClient jdbc;
    @MockitoBean GithubClient github;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> database);
        properties.add("logging.level.root", () -> "WARN");
    }

    @BeforeEach
    void reset() {
        jdbc.sql("DELETE FROM releases").update();
        jdbc.sql("DELETE FROM ticket_templates").update();
        jdbc.sql("DELETE FROM secrets").update();
        tickets.clear();
        jiraStatus.set(201);
        fetchedTag.set("v1");
        when(github.getRepository(anyString(), any())).thenAnswer(invocation -> fetchedTag.get());
        request("POST", "/api/secrets", Map.of("jira_url", "http://127.0.0.1:" + jira.getAddress().getPort(),
                "jira_email", "test@example.com", "jira_api_token", "test-token"), 200);
        request("POST", "/api/templates", Map.of("name", "issue", "project_key", "TEST",
                "issue_type", "Task", "summary", "Release {tag}"), 201);
    }

    @ParameterizedTest
    @CsvSource({
            "v2.0,v1.9,false",
            "V1.9.0,v1.10.0,true",
            "v1.2.0,V1.2.0,false",
            "1.2,1.2.0,false",
            "1.2.0,1.2,false",
            "1.2,1.2.1,true",
            "1.2.1,1.2,false",
            "1.02.0,1.2,false",
            "1.2.0.0,1.2.0.1,true",
            "999999999999999999999999999999,1000000000000000000000000000000,true",
            "1000000000000000000000000000000,999999999999999999999999999999,false",
            "release-2026,release-2027,true",
            "release-2026,release-2026,false",
            "2.0,1.9-rc.1,true",
            "1.2.0-rc.1,1.0,true",
            "1.2.0,1.2.0+build.5,true",
            "1.2.0+build.5,1.2.0,true",
            "1.2,1.2.,true",
            "vv1,v1,true"
    })
    void higherVersionOnlyProcessesQualifyingFetchedReleases(String recorded, String fetched, boolean updated) {
        fetchedTag.set(recorded);
        request("POST", "/api/repos", repositoryBody("HIGHER_ONLY"), 201);
        fetchedTag.set(fetched);
        var result = request("POST", "/api/repos/check", null, 200).path("results").get(0);
        assertThat(result.path("updated").asBoolean()).isEqualTo(updated);
        assertThat(listedRepository().path("latest_tag").asText()).isEqualTo(updated ? fetched : recorded);
        assertThat(tickets).hasSize(updated ? 1 : 0);
        if (updated) {
            assertThat(result.path("jira_ticket").asText()).isEqualTo("TEST-1");
            assertThat(json.readTree(tickets.getFirst()).path("fields").path("summary").asText()).isEqualTo("Release " + fetched);
        }
    }

    @ParameterizedTest
    @CsvSource({"v2,v1,true", "v1,V1,true", "v1,v1,false", "release-a,release-b,true"})
    void omittedModeDefaultsToAnyDifferentVersionAndUsesExactTags(String recorded, String fetched, boolean updated) {
        var body = new LinkedHashMap<>(repositoryBody("ANY_DIFFERENT"));
        body.remove("version_comparison");
        fetchedTag.set(recorded);
        var created = request("POST", "/api/repos", body, 201);
        assertThat(created.path("version_comparison").asText()).isEqualTo("ANY_DIFFERENT");
        assertThat(listedRepository().path("version_comparison").asText()).isEqualTo("ANY_DIFFERENT");
        fetchedTag.set(fetched);
        var result = request("POST", "/api/repos/check", null, 200).path("results").get(0);
        assertThat(result.path("updated").asBoolean()).isEqualTo(updated);
        assertThat(listedRepository().path("latest_tag").asText()).isEqualTo(updated ? fetched : recorded);
        assertThat(tickets).hasSize(updated ? 1 : 0);
    }

    @Test
    void invalidComparisonModeIsRejectedWithoutChangingRepository() {
        request("POST", "/api/repos", repositoryBody("UNKNOWN"), 400);
        assertThat(request("GET", "/api/repos", null, 200).size()).isZero();
        request("POST", "/api/repos", repositoryBody("HIGHER_ONLY"), 201);
        request("PUT", "/api/repos/alpha", repositoryBody("UNKNOWN"), 400);
        assertThat(listedRepository().path("version_comparison").asText()).isEqualTo("HIGHER_ONLY");
    }

    @Test
    void qualifyingReleaseWithoutTemplateIsRecordedWithoutTicket() {
        var body = new LinkedHashMap<>(repositoryBody("HIGHER_ONLY"));
        body.remove("template");
        request("POST", "/api/repos", body, 201);
        fetchedTag.set("V2.0");
        var result = request("POST", "/api/repos/check", null, 200).path("results").get(0);
        assertThat(result.path("updated").asBoolean()).isTrue();
        assertThat(listedRepository().path("latest_tag").asText()).isEqualTo("V2.0");
        assertThat(tickets).isEmpty();
    }

    @Test
    void fallbackReleaseIsRecordedOnlyAfterRequiredTicketWorkSucceeds() {
        fetchedTag.set("2.0");
        request("POST", "/api/repos", repositoryBody("HIGHER_ONLY"), 201);
        fetchedTag.set("1.9-rc.1");
        jiraStatus.set(500);
        var failed = request("POST", "/api/repos/check", null, 200).path("results").get(0);
        assertThat(failed.path("error").asBoolean()).isTrue();
        assertThat(failed.path("updated").asBoolean()).isFalse();
        assertThat(listedRepository().path("latest_tag").asText()).isEqualTo("2.0");
        jiraStatus.set(201);
        var retried = request("POST", "/api/repos/check", null, 200).path("results").get(0);
        assertThat(retried.path("updated").asBoolean()).isTrue();
        assertThat(retried.path("jira_ticket").asText()).isEqualTo("TEST-1");
        assertThat(listedRepository().path("latest_tag").asText()).isEqualTo("1.9-rc.1");
        assertThat(tickets).hasSize(2);
    }

    @AfterAll static void stopJira() { jira.stop(0); }

    @Test
    void savedComparisonModeIsReturnedAndEditingStillResetsRecordedReleaseWithoutTickets() {
        var created = request("POST", "/api/repos", repositoryBody("HIGHER_ONLY"), 201);
        assertThat(created.path("version_comparison").asText()).isEqualTo("HIGHER_ONLY");
        assertThat(listedRepository().path("version_comparison").asText()).isEqualTo("HIGHER_ONLY");
        fetchedTag.set("v2");
        var edited = request("PUT", "/api/repos/alpha", repositoryBody("ANY_DIFFERENT"), 200);
        assertThat(edited.path("version_comparison").asText()).isEqualTo("ANY_DIFFERENT");
        assertThat(listedRepository().path("latest_tag").asText()).isEqualTo("v2");
        assertThat(tickets).isEmpty();
    }

    Map<String, Object> repositoryBody(String mode) {
        return Map.of("name", "alpha", "github_url", "https://github.com/example/alpha", "template", "issue", "version_comparison", mode);
    }

    JsonNode listedRepository() { return request("GET", "/api/repos", null, 200).get(0); }

    JsonNode request(String method, String path, Object body, int status) {
        try {
            String payload = body == null ? "" : json.writeValueAsString(body);
            var response = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(payload)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
            return json.readTree(response.body());
        } catch (Exception exception) { throw new RuntimeException(exception); }
    }

    static String temporaryDatabase() {
        try { return "jdbc:sqlite:" + Files.createTempDirectory("version-comparison-").resolve("test.db"); }
        catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }

    static HttpServer jiraStub() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                tickets.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = "{\"key\":\"TEST-1\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(jiraStatus.get(), response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
}
