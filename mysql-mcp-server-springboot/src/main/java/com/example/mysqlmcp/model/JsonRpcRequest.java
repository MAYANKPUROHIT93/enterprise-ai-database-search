package com.example.mysqlmcp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonRpcRequest {
    private String jsonrpc = "2.0";
    private JsonNode id;
    private String method;
    private JsonNode params;

    public JsonRpcRequest() {}

    public JsonRpcRequest(JsonNode id, String method, JsonNode params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }

    public JsonNode getId() { return id; }
    public void setId(JsonNode id) { this.id = id; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public JsonNode getParams() { return params; }
    public void setParams(JsonNode params) { this.params = params; }
}
