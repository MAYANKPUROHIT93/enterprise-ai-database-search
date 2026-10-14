package com.example.mysqlmcp.transport;

import com.example.mysqlmcp.model.JsonRpcRequest;
import com.example.mysqlmcp.model.JsonRpcResponse;
import com.example.mysqlmcp.service.ToolRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "mcp.transport.sse.enabled", havingValue = "true", matchIfMissing = true)
public class SseMcpController {

    private static final Logger log = LoggerFactory.getLogger(SseMcpController.class);

    private final ToolRegistryService registryService;
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public SseMcpController(ToolRegistryService registryService) {
        this.registryService = registryService;
    }

    /**
     * MCP SSE endpoint (RFC standard):
     * Emits the endpoint URL for subsequent messages and maintains long-lived event stream.
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter handleSseStream() {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L); // Infinite timeout

        activeEmitters.put(sessionId, emitter);

        emitter.onCompletion(() -> activeEmitters.remove(sessionId));
        emitter.onTimeout(() -> activeEmitters.remove(sessionId));
        emitter.onError((e) -> activeEmitters.remove(sessionId));

        try {
            // MCP protocol requires sending the endpoint URI as the first SSE event
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/mcp/message?sessionId=" + sessionId));
            log.info("SSE client connected with session ID: {}", sessionId);
        } catch (IOException e) {
            log.error("Failed to send initial endpoint event: {}", e.getMessage());
            emitter.complete();
        }

        return emitter;
    }

    /**
     * MCP Message handler: Accepts incoming JSON-RPC requests via HTTP POST.
     */
    @PostMapping(value = "/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonRpcResponse> handleMessage(@RequestBody JsonRpcRequest request,
                                                         @RequestParam(required = false) String sessionId) {
        JsonRpcResponse response = registryService.handleRequest(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for HTTP monitors
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "mysql-mcp-server-springboot",
                "activeSseSessions", activeEmitters.size()
        ));
    }
}
