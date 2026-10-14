package com.example.mysqlmcp;

import com.example.mysqlmcp.model.JsonRpcRequest;
import com.example.mysqlmcp.model.JsonRpcResponse;
import com.example.mysqlmcp.service.ToolRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "mcp.transport.stdio.enabled=false",
        "mcp.transport.sse.enabled=false"
})
class MysqlMcpServerApplicationTests {

    @Autowired
    private ToolRegistryService toolRegistryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testInitializeMethod() {
        JsonRpcRequest req = new JsonRpcRequest(objectMapper.valueToTree(1), "initialize", null);
        JsonRpcResponse resp = toolRegistryService.handleRequest(req);

        assertNotNull(resp);
        assertEquals(1, resp.getId().asInt());
        assertNull(resp.getError());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        assertEquals("2024-11-05", result.get("protocolVersion"));

        @SuppressWarnings("unchecked")
        Map<String, Object> serverInfo = (Map<String, Object>) result.get("serverInfo");
        assertEquals("mysql-mcp-server-springboot", serverInfo.get("name"));
    }

    @Test
    void testToolsListMethod() {
        JsonRpcRequest req = new JsonRpcRequest(objectMapper.valueToTree(2), "tools/list", null);
        JsonRpcResponse resp = toolRegistryService.handleRequest(req);

        assertNotNull(resp);
        assertNull(resp.getError());

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resp.getResult();
        @SuppressWarnings("unchecked")
        List<Object> tools = (List<Object>) result.get("tools");

        assertEquals(16, tools.size(), "Should have all 16 MySQL MCP tools registered");
    }

    @Test
    void testResourcesListMethod() {
        JsonRpcRequest req = new JsonRpcRequest(objectMapper.valueToTree(3), "resources/list", null);
        JsonRpcResponse resp = toolRegistryService.handleRequest(req);

        assertNotNull(resp);
        assertNull(resp.getError());
    }
}
