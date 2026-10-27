package com.felixcasa.backendjava.service;
import com.felixcasa.backendjava.client.*;
import com.felixcasa.backendjava.repository.*;
import com.felixcasa.backendjava.model.VersionComparisonMode;
import com.felixcasa.backendjava.response.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
class DockerCheckCycleTest {
    @Mock RepoRepository repos;
    @Mock DockerImageRepository images;
    @Mock DockerRegistryClient registry;
    @Mock GithubClient github;
    @Mock JiraService jira;
    @Mock SystemService system;
    @InjectMocks RepoService service;
    @ParameterizedTest @CsvSource({"v2,new,true,true", "v2,old,true,false", "v1,new,false,true", "v1,old,false,false"})
    void linkedImageAndGithubChangesCreateOnlyTheRequiredTicket(String tag, String digest, boolean repoChanged, boolean dockerChanged) {
        var repo = new RepositoryResponse("app","url","v1","github-template",VersionComparisonMode.ANY_DIFFERENT,"nginx","old","docker-template");
        when(repos.listRepo("app")).thenReturn(Optional.of(repo));
        when(repos.getLatestWrittenRelease("app")).thenReturn(Optional.of("v1"));
        when(github.getRepository(eq("url"), nullable(String.class))).thenReturn(tag);
        when(registry.fetchLatestDigest("nginx")).thenReturn(digest);
        var result = service.isLatestVersion("app");
        assertThat(result.updated()).isEqualTo(repoChanged || dockerChanged);
        if (repoChanged) {
            verify(jira).createJiraTicket("github-template","app",tag,"url");
            verify(repos).writeLatestRelease("app",tag);
        }
        if (dockerChanged && !repoChanged) verify(jira).createDockerJiraTicket("docker-template","nginx",digest);
        if (dockerChanged) verify(images).writeDigest("nginx",digest);
        verifyNoMoreInteractions(jira);
    }
    @Test void standaloneImagesAreIncludedAndFailuresDoNotAdvanceDigest() {
        when(repos.listRepos()).thenReturn(List.of());
        when(images.listStandalone()).thenReturn(List.of(new DockerImageResponse("nginx","old","docker"),new DockerImageResponse("redis","old",null)));
        when(registry.fetchLatestDigest("nginx")).thenReturn("new");
        when(registry.fetchLatestDigest("redis")).thenReturn("new");
        when(jira.createDockerJiraTicket("docker","nginx","new")).thenThrow(new RuntimeException("unavailable"));
        var results=service.checkAllRepos();
        assertThat(results).hasSize(2);
        verify(images,never()).writeDigest("nginx","new");
        verify(images).writeDigest("redis","new");
    }
    @Test void failedGithubTicketDoesNotAdvanceEitherState() {
        var repo = new RepositoryResponse("app","url","v1","github",VersionComparisonMode.ANY_DIFFERENT,"nginx","old","docker");
        when(repos.listRepo("app")).thenReturn(Optional.of(repo));
        when(repos.getLatestWrittenRelease("app")).thenReturn(Optional.of("v1"));
        when(github.getRepository(eq("url"), nullable(String.class))).thenReturn("v2");
        when(registry.fetchLatestDigest("nginx")).thenReturn("new");
        when(jira.createJiraTicket("github","app","v2","url")).thenThrow(new RuntimeException());
        assertThatThrownBy(() -> service.isLatestVersion("app")).isInstanceOf(RuntimeException.class);
        verify(repos,never()).writeLatestRelease(anyString(),anyString());
        verify(images,never()).writeDigest(anyString(),anyString());
    }

    private void linkedRepository() {
        var repo = new RepositoryResponse("app", "url", "v1", "github", VersionComparisonMode.ANY_DIFFERENT,
                "nginx", "old", "docker");
        when(repos.listRepo("app")).thenReturn(Optional.of(repo));
    }

    @Test void linkedRegistryFailureDoesNotBlockQualifyingGithubRelease() {
        linkedRepository();
        when(repos.getLatestWrittenRelease("app")).thenReturn(Optional.of("v1"));
        when(github.getRepository(eq("url"), nullable(String.class))).thenReturn("v2");
        when(registry.fetchLatestDigest("nginx")).thenThrow(new RuntimeException("registry unavailable"));
        when(jira.createJiraTicket("github", "app", "v2", "url")).thenReturn("GH-1");

        assertThat(service.isLatestVersion("app")).isEqualTo(new IsLatestVersionResponse(true, "GH-1"));
        verify(repos).writeLatestRelease("app", "v2");
        verify(images, never()).writeDigest(anyString(), anyString());
        verifyNoMoreInteractions(jira);
    }

    @Test void githubFetchFailureDoesNotBlockChangedLinkedImage() {
        linkedRepository();
        when(github.getRepository(eq("url"), nullable(String.class))).thenThrow(new RuntimeException("GitHub unavailable"));
        when(registry.fetchLatestDigest("nginx")).thenReturn("new");
        when(jira.createDockerJiraTicket("docker", "nginx", "new")).thenReturn("DOCKER-1");

        assertThat(service.isLatestVersion("app")).isEqualTo(new IsLatestVersionResponse(true, "DOCKER-1"));
        verify(images).writeDigest("nginx", "new");
        verify(repos, never()).writeLatestRelease(anyString(), anyString());
        verifyNoMoreInteractions(jira);
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void unchangedSurvivingSourceReportsFetchFailure(boolean githubFails) {
        linkedRepository();
        var failure = new RuntimeException("fetch failed");
        if (githubFails) {
            when(github.getRepository(eq("url"), nullable(String.class))).thenThrow(failure);
            when(registry.fetchLatestDigest("nginx")).thenReturn("old");
        } else {
            when(github.getRepository(eq("url"), nullable(String.class))).thenReturn("v1");
            when(repos.getLatestWrittenRelease("app")).thenReturn(Optional.of("v1"));
            when(registry.fetchLatestDigest("nginx")).thenThrow(failure);
        }
        assertThatThrownBy(() -> service.isLatestVersion("app")).isSameAs(failure);
        verifyNoInteractions(jira);
        verify(images, never()).writeDigest(anyString(), anyString());
        verify(repos, never()).writeLatestRelease(anyString(), anyString());
    }

    @Test void bothFetchFailuresAreReported() {
        linkedRepository();
        var githubFailure = new RuntimeException("GitHub unavailable");
        var registryFailure = new RuntimeException("registry unavailable");
        when(github.getRepository(eq("url"), nullable(String.class))).thenThrow(githubFailure);
        when(registry.fetchLatestDigest("nginx")).thenThrow(registryFailure);
        assertThatThrownBy(() -> service.isLatestVersion("app")).isSameAs(githubFailure)
                .hasSuppressedException(registryFailure);
        verifyNoInteractions(jira);
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void survivingChangedSourceTicketFailureIsReportedWithoutAdvancingState(boolean githubFails) {
        linkedRepository();
        var ticketFailure = new RuntimeException("Jira unavailable");
        if (githubFails) {
            when(github.getRepository(eq("url"), nullable(String.class))).thenThrow(new RuntimeException("GitHub unavailable"));
            when(registry.fetchLatestDigest("nginx")).thenReturn("new");
            when(jira.createDockerJiraTicket("docker", "nginx", "new")).thenThrow(ticketFailure);
        } else {
            when(github.getRepository(eq("url"), nullable(String.class))).thenReturn("v2");
            when(repos.getLatestWrittenRelease("app")).thenReturn(Optional.of("v1"));
            when(registry.fetchLatestDigest("nginx")).thenThrow(new RuntimeException("registry unavailable"));
            when(jira.createJiraTicket("github", "app", "v2", "url")).thenThrow(ticketFailure);
        }
        assertThatThrownBy(() -> service.isLatestVersion("app")).isSameAs(ticketFailure);
        verify(images, never()).writeDigest(anyString(), anyString());
        verify(repos, never()).writeLatestRelease(anyString(), anyString());
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void concurrentCheckCyclesShareResultsAndAllowFreshCycleAfterCompletion(boolean fails) throws Exception {
        var enteredCycle = new CountDownLatch(1);
        var releaseCycle = new CountDownLatch(1);
        var secondCallerStarted = new CountDownLatch(1);
        var failure = new RuntimeException("cycle failed");
        when(repos.listRepos()).thenAnswer(invocation -> {
            enteredCycle.countDown();
            if (!releaseCycle.await(5, TimeUnit.SECONDS)) throw new AssertionError("Cycle was not released");
            if (fails) throw failure;
            return List.of();
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(service::checkAllRepos);
            assertThat(enteredCycle.await(5, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> {
                secondCallerStarted.countDown();
                return service.checkAllRepos();
            });
            assertThat(secondCallerStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            verify(repos, times(1)).listRepos();
            releaseCycle.countDown();
            if (fails) {
                assertThatThrownBy(() -> first.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class).hasCause(failure);
                assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class).hasCause(failure);
            } else {
                assertThat(second.get(5, TimeUnit.SECONDS)).isSameAs(first.get(5, TimeUnit.SECONDS));
            }
            doReturn(List.of()).when(repos).listRepos();
            assertThat(service.checkAllRepos()).isEmpty();
            verify(repos, times(2)).listRepos();
        } finally {
            releaseCycle.countDown();
            executor.shutdownNow();
        }
    }
}
