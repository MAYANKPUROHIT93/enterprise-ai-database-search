package com.example.orchestrator.controller;

import com.example.orchestrator.client.GeminiClientService;
import com.example.orchestrator.client.McpClientService;
import com.example.orchestrator.dto.SearchRequest;
import com.example.orchestrator.dto.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);
    private static final Pattern RAW_SQL_PATTERN =
            Pattern.compile("^\\s*(SELECT\\s+.+?\\s+FROM\\s+|SHOW\\s+(TABLES|DATABASES|COLUMNS|CREATE|STATUS|VARIABLES|PROCESSLIST|INDEX|GRANTS)|DESCRIBE\\s+\\w+|DESC\\s+\\w+|EXPLAIN\\s+(SELECT|ANALYZE)|INSERT\\s+INTO\\s+|UPDATE\\s+\\w+\\s+SET\\s+|DELETE\\s+FROM\\s+|CREATE\\s+(TABLE|DATABASE)|ALTER\\s+TABLE)\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final GeminiClientService geminiService;
    private final McpClientService mcpService;

    public SearchController(GeminiClientService geminiService, McpClientService mcpService) {
        this.geminiService = geminiService;
        this.mcpService = mcpService;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        long startTime = System.currentTimeMillis();
        String query = (request != null && request.getQuery() != null) ? request.getQuery().trim() : "";

        if (query.isEmpty()) {
            return ResponseEntity.badRequest().body(SearchResponse.ofError("", "Query cannot be empty."));
        }

        try {
            // 1. Determine if input is raw SQL or natural language
            String sql;
            if (RAW_SQL_PATTERN.matcher(query).find()) {
                sql = query;
                log.info("Direct SQL detected: {}", sql);
            } else {
                log.info("Natural language query received: '{}'. Generating SQL with Gemini...", query);
                sql = geminiService.generateSql(query);
                log.info("Gemini generated SQL: {}", sql);
            }

            // 2. Execute SQL through Spring Boot MCP Server
            log.info("Executing SQL via Spring Boot MCP Server (database: {})...", mcpService.getDatabase());
            Map<String, Object> mcpResult = mcpService.executeQuery(sql);

            if (mcpResult.containsKey("error")) {
                String errorMsg = (String) mcpResult.get("error");
                return ResponseEntity.ok(SearchResponse.ofError(query, "MCP Error: " + errorMsg));
            }

            // 3. Extract results
            String markdown = (String) mcpResult.get("markdown");
            @SuppressWarnings("unchecked")
            List<String> columns = (List<String>) mcpResult.get("columns");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) mcpResult.get("rows");
            Integer rowCount = (Integer) mcpResult.get("rowCount");
            if (rowCount == null && rows != null) rowCount = rows.size();

            // 4. Generate AI summary
            String answer = geminiService.generateSummary(query, sql, (markdown != null) ? markdown : rows);

            long totalExecutionTime = System.currentTimeMillis() - startTime;

            SearchResponse response = SearchResponse.ofSuccess(
                    query,
                    answer,
                    sql,
                    markdown,
                    columns != null ? columns : List.of(),
                    rows != null ? rows : List.of(),
                    rowCount != null ? rowCount : 0,
                    totalExecutionTime,
                    geminiService.getActiveModel(),
                    "mysql-mcp-server-springboot (:8085)"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Search processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    SearchResponse.ofError(query, "Error processing query: " + e.getMessage())
            );
        }
    }

    @GetMapping("/schema")
    public ResponseEntity<Map<String, Object>> getSchema() {
        Map<String, Object> tables = mcpService.listTables();
        Map<String, Object> empDesc = mcpService.describeTable("employee");
        Map<String, Object> deptDesc = mcpService.describeTable("department");

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("database", mcpService.getDatabase());
        schema.put("tables", tables.get("tables"));
        schema.put("employeeColumns", empDesc.get("columns"));
        schema.put("departmentColumns", deptDesc.get("columns"));

        return ResponseEntity.ok(schema);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> ping = mcpService.ping();
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "spring-ai-mcp-orchestrator",
                "geminiModel", geminiService.getActiveModel(),
                "mcpServerPing", ping
        ));
    }
}
