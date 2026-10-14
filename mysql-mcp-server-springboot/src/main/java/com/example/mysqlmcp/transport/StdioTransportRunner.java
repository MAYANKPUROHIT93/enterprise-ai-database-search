package com.example.mysqlmcp.transport;

import com.example.mysqlmcp.model.JsonRpcRequest;
import com.example.mysqlmcp.model.JsonRpcResponse;
import com.example.mysqlmcp.service.ToolRegistryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

@Component
public class StdioTransportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StdioTransportRunner.class);

    private final ToolRegistryService registryService;
    private final ObjectMapper objectMapper;

    @Value("${mcp.transport.stdio.enabled:true}")
    private boolean stdioEnabled;

    public StdioTransportRunner(ToolRegistryService registryService, ObjectMapper objectMapper) {
        this.registryService = registryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        if (!stdioEnabled) {
            log.info("MCP Stdio transport is disabled.");
            return;
        }

        // Spawn a background thread to listen to System.in
        Thread stdioThread = new Thread(this::listenStdio, "mcp-stdio-listener");
        stdioThread.setDaemon(true);
        stdioThread.start();
        log.info("MCP Stdio transport listener started and listening on System.in");
    }

    private void listenStdio() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintStream stdout = new PrintStream(System.out, true, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    JsonRpcRequest req = objectMapper.readValue(line, JsonRpcRequest.class);
                    JsonRpcResponse resp = registryService.handleRequest(req);

                    if (resp != null) {
                        String jsonResp = objectMapper.writeValueAsString(resp);
                        synchronized (System.out) {
                            stdout.println(jsonResp);
                            stdout.flush();
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse JSON-RPC line: {}", line, e);
                    JsonRpcResponse errResp = JsonRpcResponse.error(null, -32700, "Parse error: " + e.getMessage());
                    String jsonResp = objectMapper.writeValueAsString(errResp);
                    synchronized (System.out) {
                        stdout.println(jsonResp);
                        stdout.flush();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Stdio transport closed: {}", e.getMessage());
        }
    }
}
