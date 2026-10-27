package com.felixcasa.backendjava.repository;
import com.felixcasa.backendjava.response.RegistryResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class RegistryRepository {
    private final JdbcClient jdbc;
    public RegistryRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public record RegistryCredential(String username, String token) {
        @Override public String toString() { return "RegistryCredential[REDACTED]"; }
    }
    public Optional<RegistryCredential> find(String registry) {
        return jdbc.sql("SELECT username, token FROM registry_credentials WHERE registry = ?").param(registry)
            .query((rs,n) -> new RegistryCredential(rs.getString(1), rs.getString(2))).optional();
    }
    public List<RegistryResponse> list() {
        return jdbc.sql("SELECT registry, username FROM registry_credentials ORDER BY registry")
            .query((rs,n) -> new RegistryResponse(rs.getString(1), rs.getString(2), true)).list();
    }
    public void save(String registry, String username, String token) {
        jdbc.sql("INSERT INTO registry_credentials VALUES (?, ?, ?) ON CONFLICT(registry) DO UPDATE SET username=excluded.username, token=excluded.token")
            .params(registry, username, token).update();
    }
    public boolean delete(String registry) {
        return jdbc.sql("DELETE FROM registry_credentials WHERE registry=?").param(registry).update() > 0;
    }
}
