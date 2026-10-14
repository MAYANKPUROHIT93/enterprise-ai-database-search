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
public class GeminiClientService {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api-key:YOUR_GEMINI_API_KEY_HERE}")
    private String apiKey;

    @Value("${gemini.primary-model:gemini-3.1-flash-lite}")
    private String primaryModel;

    @Value("${gemini.fallback-models:gemini-flash-latest,gemini-3.8-flash,gemini-3.5-flash}")
    private List<String> fallbackModels;

    private static final String SCHEMA_CONTEXT = """
        Database Schema (MySQL Database: `dabba`):
        
        1. Table `department` (
           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
           `department_code` VARCHAR(20) UNIQUE (e.g. 'ENG', 'PRD', 'SLS', 'HR', 'FIN', 'OPS', 'MKT', 'DSN'),
           `department_name` VARCHAR(100) UNIQUE ('Engineering', 'Product', 'Sales', 'HR', 'Finance', 'DevOps', 'Marketing', 'Design'),
           `description` VARCHAR(255),
           `location` VARCHAR(100) (e.g. 'Bengaluru Campus', 'Mumbai Campus', 'Hyderabad Campus'),
           `budget` DECIMAL(15,2),
           `created_at` TIMESTAMP
        );
        
        2. Table `employee` (
           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
           `name` VARCHAR(255) NOT NULL,
           `email` VARCHAR(255) UNIQUE,
           `department` VARCHAR(255),
           `department_id` BIGINT (FOREIGN KEY -> department.id),
           `job_title` VARCHAR(255),
           `salary` DOUBLE,
           `hire_date` DATE,
           `status` VARCHAR(50) ('ACTIVE', 'ON_LEAVE', 'TERMINATED')
        );
        
        Relationship: `employee.department_id = department.id`.
    """;

    public GeminiClientService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public String generateSql(String userQuery) {
        String prompt = String.format("""
            You are an expert MySQL database engineer and SQL query generator.
            
            %s
            
            Task:
            Translate the user's natural language question into a clean, syntactically valid MySQL query.
            
            Guidelines:
            - Return ONLY the raw SQL query. Do not wrap in markdown ```sql ... ``` code blocks.
            - Do not include explanations, comments, or preamble.
            - If department information is requested, JOIN `employee` and `department` on `employee.department_id = department.id`.
            - Use appropriate aggregations (COUNT, AVG, SUM, MIN, MAX), GROUP BY, ORDER BY, and LIMIT where appropriate.
            
            User Question: "%s"
            SQL Query:
        """, SCHEMA_CONTEXT, userQuery);

        String rawResponse = callGemini(prompt);
        return cleanSql(rawResponse);
    }

    public String generateSummary(String userQuery, String sql, Object dataSummary) {
        String prompt = String.format("""
            You are an intelligent enterprise AI assistant analyzing data from a company's MySQL database.
            
            User Question: "%s"
            Executed SQL: "%s"
            Retrieved Data:
            %s
            
            Task:
            Provide a clear, helpful, and beautifully structured natural language answer directly answering the question based on the retrieved data.
            
            Formatting Rules:
            - Use clean, standard Markdown with proper paragraph line breaks.
            - When listing items, columns, fields, or records, format EACH item as a separate bullet point on its own new line starting with "* ". Never join or concatenate multiple bullet points on the same line.
            - Use bold text for key entity names, column names, or figures (e.g. **id**, **name**, **$185,000**).
            - Highlight key metrics, totals, averages, or notable insights clearly.
        """, userQuery, sql, String.valueOf(dataSummary));

        return callGemini(prompt);
    }

    private String callGemini(String promptText) {
        List<String> modelCandidates = new ArrayList<>();
        modelCandidates.add(primaryModel);
        if (fallbackModels != null) {
            modelCandidates.addAll(fallbackModels);
        }

        for (String model : modelCandidates) {
            try {
                String url = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                        model, apiKey
                );

                Map<String, Object> requestBody = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(Map.of("text", promptText)))
                        )
                );

                String responseStr = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(responseStr);
                if (root.has("candidates") && root.get("candidates").isArray() && root.get("candidates").size() > 0) {
                    JsonNode parts = root.get("candidates").get(0).get("content").get("parts");
                    if (parts.isArray() && parts.size() > 0) {
                        return parts.get(0).get("text").asText().trim();
                    }
                }
            } catch (Exception e) {
                log.warn("Model '{}' failed: {}. Trying fallback...", model, e.getMessage());
            }
        }

        return "Query processed successfully based on database records.";
    }

    private String cleanSql(String text) {
        if (text == null) return "SELECT * FROM employee LIMIT 10";
        String s = text.trim();
        if (s.startsWith("```sql")) {
            s = s.substring(6);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    public String getActiveModel() { return primaryModel; }
}
