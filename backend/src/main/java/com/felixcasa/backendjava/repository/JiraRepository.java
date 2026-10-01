package com.felixcasa.backendjava.repository;

import com.felixcasa.backendjava.request.TicketTemplateRequest;
import com.felixcasa.backendjava.response.RepositoryResponse;
import com.felixcasa.backendjava.response.TicketTemplateResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JiraRepository {
    private final JdbcClient jdbcClient;

    public JiraRepository(JdbcClient jdbcClient){
        this.jdbcClient = jdbcClient;
    }

    public boolean addTicketTemplate(TicketTemplateRequest request){
        try {
            if ((this.jdbcClient.sql("SELECT 1 FROM ticket_templates WHERE name = :name LIMIT 1").param("name", request.name()).query(String.class).optional()).isPresent()) {
                return false;
            } else {
                this.jdbcClient
                        .sql("INSERT INTO ticket_templates (name, project_key, issue_type, summary, description,assignee_id, priority, due_date, labels, transition_name) VALUES (:name, :projectKey, :issueType, :summary, :description, :assigneeId, :priority, :dueDate, :labels, :transitionName)")
                        .param("name", request.name())
                        .param("projectKey", request.projectKey())
                        .param("issueType", request.issueType())
                        .param("summary", request.summary())
                        .param("description", request.description())
                        .param("assigneeId", request.assigneeId())
                        .param("priority", request.priority())
                        .param("dueDate", request.dueDate())
                        .param("labels", request.labels())
                        .param("transitionName", request.transitionName())
                        .update();
                return true;
            }
        } catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public boolean updateTicketTemplate(String originalName, TicketTemplateRequest request){
        try {
            int rowsAffected = this.jdbcClient
                    .sql("UPDATE ticket_templates SET name = :name, project_key = :projectKey, issue_type = :issueType, summary = :summary, description = :description,assignee_id = :assigneeId, priority = :priority, due_date = :dueDate, labels = :labels, transition_name = :transitionName WHERE name = :originalName")
                    .param("name", request.name())
                    .param("projectKey", request.projectKey())
                    .param("issueType", request.issueType())
                    .param("summary", request.summary())
                    .param("description", request.description())
                    .param("assigneeId", request.assigneeId())
                    .param("priority", request.priority())
                    .param("dueDate", request.dueDate())
                    .param("labels", request.labels())
                    .param("transitionName", request.transitionName())
                    .param("originalName", originalName)
                    .update();
            return rowsAffected > 0;
        }catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public boolean deleteTicketTemplate(String name){
        try {
            int rowsEffected = this.jdbcClient
                    .sql("DELETE FROM ticket_templates WHERE name = :name")
                    .param("name", name)
                    .update();
            return rowsEffected > 0;
        } catch (DataIntegrityViolationException dataIntegrityViolationException){
            return false;
        }
    }

    public Optional<TicketTemplateResponse> getTicketTemplate (String name) {
        return this.jdbcClient
                .sql("SELECT * FROM ticket_templates WHERE name = :name")
                .param("name", name)
                .query((rs, rowNum) ->
                        new TicketTemplateResponse(
                                rs.getString("name"),
                                rs.getString("project_key"),
                                rs.getString("issue_type"),
                                rs.getString("summary"),
                                rs.getString("description"),
                                rs.getString("assignee_id"),
                                rs.getString("priority"),
                                rs.getString("due_date"),
                                rs.getString("labels"),
                                rs.getString("transition_name")
                        )
                )
                .optional();
    }

    public List<TicketTemplateResponse> listTicketTemplates(){
        return this.jdbcClient
                .sql("SELECT * FROM ticket_templates ORDER BY name")
                .query((rs, rowNum) ->
                        new TicketTemplateResponse(
                                rs.getString("name"),
                                rs.getString("project_key"),
                                rs.getString("issue_type"),
                                rs.getString("summary"),
                                rs.getString("description"),
                                rs.getString("assignee_id"),
                                rs.getString("priority"),
                                rs.getString("due_date"),
                                rs.getString("labels"),
                                rs.getString("transition_name")
                        )
                )
                .list();
    }
}
