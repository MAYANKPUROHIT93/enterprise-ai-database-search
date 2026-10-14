package com.example.orchestrator.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class McpClientService {

    private static final Logger log = LoggerFactory.getLogger(McpClientService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${mcp.server-url:http://127.0.0.1:8085/mcp/message}")
    private String mcpServerUrl;

    @Value("${mcp.database:dabba}")
    private String database;

    public McpClientService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> executeQuery(String sql) {
        Map<String, Object> toolArgs = new LinkedHashMap<>();
        toolArgs.put("sql", sql);
        toolArgs.put("database", database);
        toolArgs.put("format", "markdown");

        return callTool("mysql_execute_query", toolArgs);
    }

    public Map<String, Object> listTables() {
        return callTool("mysql_list_tables", Map.of("database", database));
    }

    public Map<String, Object> describeTable(String table) {
        return callTool("mysql_describe_table", Map.of("table", table, "database", database));
    }

    public Map<String, Object> ping() {
        return callTool("mysql_ping", Map.of());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> rpcPayload = new LinkedHashMap<>();
        rpcPayload.put("jsonrpc", "2.0");
        rpcPayload.put("id", System.currentTimeMillis());
        rpcPayload.put("method", "tools/call");
        rpcPayload.put("params", Map.of(
                "name", toolName,
                "arguments", arguments != null ? arguments : Map.of()
        ));

        try {
            String rawJson = restClient.post()
                    .uri(mcpServerUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(rpcPayload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            if (root.has("result") && root.get("result").has("content")) {
                JsonNode contentArray = root.get("result").get("content");
                if (contentArray.isArray() && contentArray.size() > 0) {
                    String text = contentArray.get(0).get("text").asText();
                    try {
                        return objectMapper.readValue(text, Map.class);
                    } catch (Exception e) {
                        return Map.of("rawOutput", text);
                    }
                }
            } else if (root.has("error")) {
                return Map.of("error", root.get("error").get("message").asText());
            }

            return Map.of("response", rawJson);
        } catch (Exception e) {
            log.error("Failed to call Spring Boot MCP tool '{}': {}", toolName, e.getMessage());
            return Map.of("error", "MCP call failed: " + e.getMessage());
        }
    }

    public String getDatabase() { return database; }
    public String getMcpServerUrl() { return mcpServerUrl; }
}
