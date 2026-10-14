# Full-Grade MySQL Model Context Protocol (MCP) Server in Spring Boot

A production-ready, enterprise **Model Context Protocol (MCP)** server for **MySQL** databases built with **Spring Boot 3.3.4**, **Java 17**, and **HikariCP** high-performance connection pooling.

Supports both **Stdio Transport** (for local AI assistants: Antigravity IDE, Claude Desktop, Cursor, Cline) and **Server-Sent Events (SSE) / HTTP Transport** (for remote AI services and microservices).

---

## 🚀 Key Features

- **Enterprise Connection Pooling**: Powered by HikariCP with connection validation, idle timeouts, and automatic reconnection.
- **Dual Transport Mechanisms**:
  - **Stdio Transport**: Reads JSON-RPC 2.0 lines from `System.in`, returns responses to `System.out`, and routes framework logs to `System.err`.
  - **HTTP & SSE Transport**: Exposes `/mcp/sse` (Server-Sent Events) and `/mcp/message` (HTTP POST) for remote agent integrations.
- **All 16 MySQL MCP Tools Supported**:
  1. `mysql_ping`: Health check and latency test.
  2. `mysql_list_databases`: Schemas, character sets, and collations.
  3. `mysql_list_tables`: Tables and views with storage size (data/index MB) and row counts.
  4. `mysql_describe_table`: Column types, nullability, keys, defaults, and comments.
  5. `mysql_show_create_table`: Exact `SHOW CREATE TABLE` DDL extraction.
  6. `mysql_list_indexes`: Primary, unique, and secondary indexes with cardinality.
  7. `mysql_list_foreign_keys`: Foreign key constraints and parent references.
  8. `mysql_execute_query`: SQL queries (SELECT, INSERT, UPDATE, DELETE, DDL) with pagination and Markdown formatting.
  9. `mysql_explain_query`: `EXPLAIN FORMAT=JSON` and `EXPLAIN ANALYZE` execution plans.
  10. `mysql_execute_transaction`: Multi-statement atomic ACID transactions with rollback.
  11. `mysql_batch_execute`: Multi-statement SQL scripts (schema creations, migrations).
  12. `mysql_server_status`: Live uptime, active threads, QPS, slow query counters.
  13. `mysql_database_stats`: Storage analysis per database.
  14. `mysql_process_list`: Live active connections and query execution list.
  15. `mysql_kill_query`: Kill stalled queries or connections by ID.
  16. `mysql_open_in_workbench`: Generate SQL files and launch MySQL Workbench.
- **Safety Guardrails**: Optional `MYSQL_READ_ONLY=true` prevents mutations (INSERT, UPDATE, DELETE, DROP, ALTER).

---

## 📂 Project Structure

```
mysql-mcp-server-springboot/
├── pom.xml
├── .env.example
├── README.md
└── src/
    ├── main/
    │   ├── java/com/example/mysqlmcp/
    │   │   ├── MysqlMcpServerApplication.java
    │   │   ├── config/
    │   │   │   ├── MysqlMcpConfig.java
    │   │   │   └── DataSourceConfig.java
    │   │   ├── model/
    │   │   │   ├── JsonRpcRequest.java
    │   │   │   ├── JsonRpcResponse.java
    │   │   │   └── McpTool.java
    │   │   ├── service/
    │   │   │   ├── DatabaseService.java
    │   │   │   ├── SchemaService.java
    │   │   │   ├── QueryService.java
    │   │   │   ├── AdminService.java
    │   │   │   └── ToolRegistryService.java
    │   │   ├── transport/
    │   │   │   ├── StdioTransportRunner.java
    │   │   │   └── SseMcpController.java
    │   │   └── util/
    │   │       └── TableFormatter.java
    │   └── resources/
    │       ├── application.yml
    │       └── logback-spring.xml   # Keeps stdout clean for Stdio JSON-RPC
    └── test/
        └── java/com/example/mysqlmcp/
            └── MysqlMcpServerApplicationTests.java
```

---

## 🛠️ Build & Packaging

### 1. Requirements
- **Java 17** or higher
- **Maven 3.8+**

### 2. Compile and Package Executable JAR
```bash
cd mysql-mcp-server-springboot
mvn clean package -DskipTests
```
This produces `target/mysql-mcp-server-springboot-1.0.0.jar`.

---

## ⚙️ Configuration

Environment variables can be provided in your system or via an IDE launcher:

| Variable | Default | Description |
|:---|:---|:---|
| `MYSQL_HOST` | `127.0.0.1` | MySQL Host |
| `MYSQL_PORT` | `3305` | MySQL Port |
| `MYSQL_USER` | `root` | Database Username |
| `MYSQL_PASSWORD` | `root` | Database Password |
| `MYSQL_DATABASE` | `dabba` | Default database context |
| `MYSQL_READ_ONLY` | `false` | Block write queries if true |
| `MYSQL_MAX_ROWS` | `1000` | Max SELECT row limit |
| `MCP_STDIO_ENABLED`| `true` | Enable Stdio JSON-RPC transport |
| `MCP_SSE_ENABLED`  | `true` | Enable HTTP/SSE endpoints |
| `SERVER_PORT`      | `8085` | Web port for HTTP/SSE |

---

## 🔌 Antigravity IDE & Claude Desktop Integration

Add the Spring Boot MCP Server to `~/.gemini/config/mcp_config.json` (or `claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "mysql-springboot": {
      "command": "java",
      "args": [
        "-jar",
        "c:/Users/MAYANK/Desktop/wolf_scene/mysql-mcp-server-springboot/target/mysql-mcp-server-springboot-1.0.0.jar"
      ],
      "env": {
        "MYSQL_HOST": "127.0.0.1",
        "MYSQL_PORT": "3305",
        "MYSQL_USER": "root",
        "MYSQL_PASSWORD": "root",
        "MYSQL_DATABASE": "dabba",
        "MCP_STDIO_ENABLED": "true",
        "MCP_SSE_ENABLED": "false"
      }
    }
  }
}
```

Or for development without building the JAR first:
```json
{
  "mcpServers": {
    "mysql-springboot-dev": {
      "command": "mvn",
      "args": [
        "-f",
        "c:/Users/MAYANK/Desktop/wolf_scene/mysql-mcp-server-springboot/pom.xml",
        "spring-boot:run"
      ]
    }
  }
}
```

---

## 🌐 Remote Agent Integration (SSE)

Run the server standalone:
```bash
java -jar target/mysql-mcp-server-springboot-1.0.0.jar
```
Connect via SSE:
- **SSE Stream**: `http://localhost:8085/mcp/sse`
- **POST Messages**: `http://localhost:8085/mcp/message`
- **Health Check**: `http://localhost:8085/mcp/health`
