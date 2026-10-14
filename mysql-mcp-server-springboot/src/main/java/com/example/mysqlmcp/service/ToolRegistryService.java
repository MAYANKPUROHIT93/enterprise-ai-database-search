package com.example.mysqlmcp.service;

import com.example.mysqlmcp.model.JsonRpcRequest;
import com.example.mysqlmcp.model.JsonRpcResponse;
import com.example.mysqlmcp.model.McpTool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ToolRegistryService {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistryService.class);
    private final ObjectMapper objectMapper;
    private final DatabaseService databaseService;
    private final SchemaService schemaService;
    private final QueryService queryService;
    private final AdminService adminService;

    private final List<McpTool> tools = new ArrayList<>();

    public ToolRegistryService(ObjectMapper objectMapper,
                               DatabaseService databaseService,
                               SchemaService schemaService,
                               QueryService queryService,
                               AdminService adminService) {
        this.objectMapper = objectMapper;
        this.databaseService = databaseService;
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.adminService = adminService;
        initToolDefinitions();
    }

    private void initToolDefinitions() {
        tools.add(new McpTool(
                "mysql_ping",
                "Test the connection latency and verify that the MySQL database is reachable.",
                Map.of("type", "object", "properties", Map.of())
        ));

        tools.add(new McpTool(
                "mysql_list_databases",
                "List all accessible databases/schemas with their character set and collation.",
                Map.of("type", "object", "properties", Map.of())
        ));

        tools.add(new McpTool(
                "mysql_list_tables",
                "List all tables and views in a database with engine, approximate row count, and byte sizes.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "database", Map.of("type", "string", "description", "Database name. If omitted, uses default.")
                        )
                )
        ));

        tools.add(new McpTool(
                "mysql_describe_table",
                "Get detailed column metadata for a table (data type, nullable, primary key, default value, extra, comment).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "table", Map.of("type", "string", "description", "Table name to inspect"),
                                "database", Map.of("type", "string", "description", "Database name")
                        ),
                        "required", List.of("table")
                )
        ));

        tools.add(new McpTool(
                "mysql_show_create_table",
                "Retrieve the exact SQL DDL statement (SHOW CREATE TABLE) used to create the table or view.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "table", Map.of("type", "string", "description", "Table name"),
                                "database", Map.of("type", "string", "description", "Database name")
                        ),
                        "required", List.of("table")
                )
        ));

        tools.add(new McpTool(
                "mysql_list_indexes",
                "List all indexes (primary, unique, secondary, composite) on a table with cardinality and index types.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "table", Map.of("type", "string", "description", "Table name"),
                                "database", Map.of("type", "string", "description", "Database name")
                        ),
                        "required", List.of("table")
                )
        ));

        tools.add(new McpTool(
                "mysql_list_foreign_keys",
                "List all foreign key constraints and referenced tables/columns for a given table.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "table", Map.of("type", "string", "description", "Table name"),
                                "database", Map.of("type", "string", "description", "Database name")
                        ),
                        "required", List.of("table")
                )
        ));

        tools.add(new McpTool(
                "mysql_execute_query",
                "Execute an arbitrary SQL query (SELECT, INSERT, UPDATE, DELETE, DDL). Supports row limiting and markdown format.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of("type", "string", "description", "SQL statement to execute"),
                                "params", Map.of("type", "array", "description", "Optional prepared statement parameters"),
                                "database", Map.of("type", "string", "description", "Optional database context"),
                                "max_rows", Map.of("type", "number", "description", "Maximum rows to return"),
                                "format", Map.of("type", "string", "enum", List.of("json", "markdown"), "description", "Output format")
                        ),
                        "required", List.of("sql")
                )
        ));

        tools.add(new McpTool(
                "mysql_explain_query",
                "Execute EXPLAIN or EXPLAIN ANALYZE on a query to inspect the MySQL query execution plan.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of("type", "string", "description", "SQL query to explain"),
                                "analyze", Map.of("type", "boolean", "description", "If true, runs EXPLAIN ANALYZE"),
                                "database", Map.of("type", "string", "description", "Database name")
                        ),
                        "required", List.of("sql")
                )
        ));

        tools.add(new McpTool(
                "mysql_execute_transaction",
                "Execute multiple SQL statements sequentially inside a single atomic ACID transaction. Rolls back on any error.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "statements", Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "sql", Map.of("type", "string"),
                                                        "params", Map.of("type", "array")
                                                ),
                                                "required", List.of("sql")
                                        )
                                ),
                                "database", Map.of("type", "string")
                        ),
                        "required", List.of("statements")
                )
        ));

        tools.add(new McpTool(
                "mysql_batch_execute",
                "Execute a batch of SQL statements separated by semicolons (e.g., database schema setup or migrations).",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of("type", "string", "description", "Multiple SQL statements separated by semicolons"),
                                "database", Map.of("type", "string", "description", "Optional database context")
                        ),
                        "required", List.of("sql")
                )
        ));

        tools.add(new McpTool(
                "mysql_server_status",
                "Retrieve live MySQL server performance metrics, uptime, active connections, total queries, and slow queries.",
                Map.of("type", "object", "properties", Map.of())
        ));

        tools.add(new McpTool(
                "mysql_database_stats",
                "Show storage statistics (data size MB, index size MB, table count) for a specific database or all databases.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "database", Map.of("type", "string", "description", "Optional database name")
                        )
                )
        ));

        tools.add(new McpTool(
                "mysql_process_list",
                "Show active MySQL connections and currently executing queries.",
                Map.of("type", "object", "properties", Map.of())
        ));

        tools.add(new McpTool(
                "mysql_kill_query",
                "Terminate a specific long-running or stalled query or client connection by ID.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "process_id", Map.of("type", "number", "description", "Process/Thread ID to kill"),
                                "kill_connection", Map.of("type", "boolean", "description", "If true, terminates connection")
                        ),
                        "required", List.of("process_id")
                )
        ));

        tools.add(new McpTool(
                "mysql_open_in_workbench",
                "Save an SQL query or migration script to a file and open it inside MySQL Workbench GUI.",
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "sql", Map.of("type", "string", "description", "SQL code to open"),
                                "filename", Map.of("type", "string", "description", "Optional filename")
                        ),
                        "required", List.of("sql")
                )
        ));
    }

    public JsonRpcResponse handleRequest(JsonRpcRequest req) {
        String method = req.getMethod();
        JsonNode id = req.getId();
        JsonNode params = req.getParams();

        try {
            switch (method) {
                case "initialize":
                    return JsonRpcResponse.success(id, Map.of(
                            "protocolVersion", "2024-11-05",
                            "serverInfo", Map.of(
                                    "name", "mysql-mcp-server-springboot",
                                    "version", "1.0.0"
                            ),
                            "capabilities", Map.of(
                                    "tools", Map.of("listChanged", false),
                                    "resources", Map.of("listChanged", false),
                                    "prompts", Map.of("listChanged", false)
                            )
                    ));

                case "notifications/initialized":
                    return null; // Notifications do not have response

                case "ping":
                    return JsonRpcResponse.success(id, Map.of());

                case "tools/list":
                    return JsonRpcResponse.success(id, Map.of("tools", tools));

                case "tools/call":
                    return handleToolCall(id, params);

                case "resources/list":
                    return JsonRpcResponse.success(id, Map.of(
                            "resources", List.of(
                                    Map.of(
                                            "uri", "mysql://databases",
                                            "name", "MySQL Accessible Databases",
                                            "mimeType", "application/json",
                                            "description", "List of all databases accessible to the current user."
                                    ),
                                    Map.of(
                                            "uri", "mysql://server/status",
                                            "name", "MySQL Live Server Status",
                                            "mimeType", "application/json",
                                            "description", "Real-time MySQL performance and thread metrics."
                                    )
                            )
                    ));

                case "resources/read":
                    return handleResourceRead(id, params);

                case "prompts/list":
                    return JsonRpcResponse.success(id, Map.of(
                            "prompts", List.of(
                                    Map.of(
                                            "name", "optimize_query",
                                            "description", "Analyze a slow query execution plan with EXPLAIN and recommend indexing improvements."
                                    ),
                                    Map.of(
                                            "name", "design_schema",
                                            "description", "Design a production-grade 3NF MySQL schema with indexes and constraints."
                                    )
                            )
                    ));

                default:
                    return JsonRpcResponse.error(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            log.error("Error processing method {}: {}", method, e.getMessage(), e);
            return JsonRpcResponse.error(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    private JsonRpcResponse handleToolCall(JsonNode id, JsonNode params) {
        if (params == null || !params.has("name")) {
            return JsonRpcResponse.error(id, -32602, "Missing 'name' in tool call parameters.");
        }

        String toolName = params.get("name").asText();
        JsonNode args = params.get("arguments");

        try {
            Object result;
            switch (toolName) {
                case "mysql_ping":
                    result = databaseService.ping();
                    break;
                case "mysql_list_databases":
                    result = schemaService.listDatabases();
                    break;
                case "mysql_list_tables":
                    result = schemaService.listTables(optString(args, "database"));
                    break;
                case "mysql_describe_table":
                    result = schemaService.describeTable(reqString(args, "table"), optString(args, "database"));
                    break;
                case "mysql_show_create_table":
                    result = schemaService.showCreateTable(reqString(args, "table"), optString(args, "database"));
                    break;
                case "mysql_list_indexes":
                    result = schemaService.listIndexes(reqString(args, "table"), optString(args, "database"));
                    break;
                case "mysql_list_foreign_keys":
                    result = schemaService.listForeignKeys(reqString(args, "table"), optString(args, "database"));
                    break;
                case "mysql_execute_query":
                    result = queryService.executeQuery(
                            reqString(args, "sql"),
                            parseList(args, "params"),
                            optString(args, "database"),
                            optInt(args, "max_rows"),
                            optString(args, "format")
                    );
                    break;
                case "mysql_explain_query":
                    result = queryService.explainQuery(
                            reqString(args, "sql"),
                            optBool(args, "analyze"),
                            optString(args, "database")
                    );
                    break;
                case "mysql_execute_transaction":
                    result = queryService.executeTransaction(
                            parseStatementList(args, "statements"),
                            optString(args, "database")
                    );
                    break;
                case "mysql_batch_execute":
                    result = queryService.batchExecute(
                            reqString(args, "sql"),
                            optString(args, "database")
                    );
                    break;
                case "mysql_server_status":
                    result = adminService.getServerStatus();
                    break;
                case "mysql_database_stats":
                    result = adminService.getDatabaseStats(optString(args, "database"));
                    break;
                case "mysql_process_list":
                    result = adminService.getProcessList();
                    break;
                case "mysql_kill_query":
                    result = adminService.killQuery(
                            args.get("process_id").asLong(),
                            optBool(args, "kill_connection")
                    );
                    break;
                case "mysql_open_in_workbench":
                    result = adminService.openInWorkbench(
                            reqString(args, "sql"),
                            optString(args, "filename")
                    );
                    break;
                default:
                    return JsonRpcResponse.success(id, Map.of(
                            "content", List.of(Map.of("type", "text", "text", "Unknown tool: " + toolName)),
                            "isError", true
                    ));
            }

            String textResult = (result instanceof String)
                    ? (String) result
                    : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

            return JsonRpcResponse.success(id, Map.of(
                    "content", List.of(Map.of("type", "text", "text", textResult)),
                    "isError", false
            ));

        } catch (Exception e) {
            log.error("Tool execution error ({}): {}", toolName, e.getMessage());
            return JsonRpcResponse.success(id, Map.of(
                    "content", List.of(Map.of("type", "text", "text", "Error executing " + toolName + ": " + e.getMessage())),
                    "isError", true
            ));
        }
    }

    private JsonRpcResponse handleResourceRead(JsonNode id, JsonNode params) throws JsonProcessingException {
        String uri = optString(params, "uri");
        if ("mysql://databases".equals(uri)) {
            try {
                Map<String, Object> dbs = schemaService.listDatabases();
                return JsonRpcResponse.success(id, Map.of(
                        "contents", List.of(Map.of(
                                "uri", uri,
                                "mimeType", "application/json",
                                "text", objectMapper.writeValueAsString(dbs)
                        ))
                ));
            } catch (Exception e) {
                return JsonRpcResponse.error(id, -32603, e.getMessage());
            }
        } else if ("mysql://server/status".equals(uri)) {
            try {
                Map<String, Object> status = adminService.getServerStatus();
                return JsonRpcResponse.success(id, Map.of(
                        "contents", List.of(Map.of(
                                "uri", uri,
                                "mimeType", "application/json",
                                "text", objectMapper.writeValueAsString(status)
                        ))
                ));
            } catch (Exception e) {
                return JsonRpcResponse.error(id, -32603, e.getMessage());
            }
        }
        return JsonRpcResponse.error(id, -32602, "Resource URI not found: " + uri);
    }

    private String reqString(JsonNode node, String field) {
        if (node == null || !node.has(field)) throw new IllegalArgumentException("Missing required parameter: " + field);
        return node.get(field).asText();
    }

    private String optString(JsonNode node, String field) {
        return (node != null && node.has(field) && !node.get(field).isNull()) ? node.get(field).asText() : null;
    }

    private Integer optInt(JsonNode node, String field) {
        return (node != null && node.has(field) && !node.get(field).isNull()) ? node.get(field).asInt() : null;
    }

    private Boolean optBool(JsonNode node, String field) {
        return (node != null && node.has(field) && !node.get(field).isNull()) ? node.get(field).asBoolean() : null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseList(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return objectMapper.convertValue(node.get(field), List.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseStatementList(JsonNode node, String field) {
        if (node == null || !node.has(field)) return List.of();
        return objectMapper.convertValue(node.get(field), List.class);
    }
}
