package com.felixcasa.backendjava;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class VersionComparisonMigrationTest {
    @TempDir Path directory;

    @Test
    void existingAndNewRepositoriesDefaultToAnyDifferentVersion() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("migration.db");
        Flyway.configure().dataSource(url, null, null).target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url); var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO releases(repo, repo_url, latest_tag) VALUES ('existing', 'existing-url', 'v2')");
        }
        Flyway.configure().dataSource(url, null, null).load().migrate();
        try (var connection = DriverManager.getConnection(url); var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO releases(repo, repo_url, latest_tag) VALUES ('new', 'new-url', 'v1')");
            try (var rows = sql.executeQuery("SELECT repo, latest_tag, version_comparison FROM releases ORDER BY repo")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("repo")).isEqualTo("existing");
                assertThat(rows.getString("latest_tag")).isEqualTo("v2");
                assertThat(rows.getString("version_comparison")).isEqualTo("ANY_DIFFERENT");
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString("repo")).isEqualTo("new");
                assertThat(rows.getString("version_comparison")).isEqualTo("ANY_DIFFERENT");
            }
        }
    }
}
