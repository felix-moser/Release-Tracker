package com.felixcasa.backendjava.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SystemRepository {

    private final JdbcClient jdbcClient;

    public SystemRepository(JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
    }

    public void setSecret(String key, String value){
        // When changed db to anything else then SQLite CHANGE QUERY!!
        this.jdbcClient
                .sql("INSERT OR REPLACE INTO secrets (key, value) VALUES (:key, :value)")
                .param("key", key)
                .param("value", value)
                .update();
    }

    public Optional<String> getSecret(String key){
        return this.jdbcClient
                .sql("SELECT value FROM secrets WHERE key = :key")
                .param("key", key)
                .query(String.class)
                .optional();
    }
}
