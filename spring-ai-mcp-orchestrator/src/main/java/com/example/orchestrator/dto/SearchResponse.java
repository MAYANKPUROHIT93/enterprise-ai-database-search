package com.example.orchestrator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResponse {
    private String query;
    private String answer;
    private String sql;
    private String markdown;
    private List<String> columns;
    private List<Map<String, Object>> rows;
    private int rowCount;
    private long executionTimeMs;
    private String model;
    private String mcpServer;
    private boolean success = true;
    private String error;

    public SearchResponse() {}

    public static SearchResponse ofSuccess(String query, String answer, String sql, String markdown,
                                           List<String> columns, List<Map<String, Object>> rows,
                                           int rowCount, long executionTimeMs, String model, String mcpServer) {
        SearchResponse resp = new SearchResponse();
        resp.setQuery(query);
        resp.setAnswer(answer);
        resp.setSql(sql);
        resp.setMarkdown(markdown);
        resp.setColumns(columns);
        resp.setRows(rows);
        resp.setRowCount(rowCount);
        resp.setExecutionTimeMs(executionTimeMs);
        resp.setModel(model);
        resp.setMcpServer(mcpServer);
        resp.setSuccess(true);
        return resp;
    }

    public static SearchResponse ofError(String query, String error) {
        SearchResponse resp = new SearchResponse();
        resp.setQuery(query);
        resp.setError(error);
        resp.setSuccess(false);
        return resp;
    }

    // Getters and Setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getSql() { return sql; }
    public void setSql(String sql) { this.sql = sql; }

    public String getMarkdown() { return markdown; }
    public void setMarkdown(String markdown) { this.markdown = markdown; }

    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> columns) { this.columns = columns; }

    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }

    public int getRowCount() { return rowCount; }
    public void setRowCount(int rowCount) { this.rowCount = rowCount; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getMcpServer() { return mcpServer; }
    public void setMcpServer(String mcpServer) { this.mcpServer = mcpServer; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
