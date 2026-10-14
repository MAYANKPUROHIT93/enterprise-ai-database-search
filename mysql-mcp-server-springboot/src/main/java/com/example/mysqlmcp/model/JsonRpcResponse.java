package com.example.mysqlmcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonRpcResponse {
    private String jsonrpc = "2.0";
    private JsonNode id;
    private Object result;
    private JsonRpcError error;

    public JsonRpcResponse() {}

    public static JsonRpcResponse success(JsonNode id, Object result) {
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    public static JsonRpcResponse error(JsonNode id, int code, String message) {
        JsonRpcResponse resp = new JsonRpcResponse();
        resp.setId(id);
        resp.setError(new JsonRpcError(code, message));
        return resp;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }

    public JsonNode getId() { return id; }
    public void setId(JsonNode id) { this.id = id; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public JsonRpcError getError() { return error; }
    public void setError(JsonRpcError error) { this.error = error; }

    public static class JsonRpcError {
        private int code;
        private String message;

        public JsonRpcError() {}
        public JsonRpcError(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
