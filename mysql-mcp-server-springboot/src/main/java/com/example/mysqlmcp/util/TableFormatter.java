package com.example.mysqlmcp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableFormatter {

    public static String formatAsMarkdownTable(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return "*(No rows returned)*";
        }

        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("| ").append(String.join(" | ", columns)).append(" |\n");

        // Separator
        sb.append("|");
        for (int i = 0; i < columns.size(); i++) {
            sb.append(" --- |");
        }
        sb.append("\n");

        // Data rows
        for (Map<String, Object> row : rows) {
            sb.append("|");
            for (String col : columns) {
                Object val = row.get(col);
                String cell = (val != null) ? val.toString().replace("\n", " ").replace("|", "\\|") : "NULL";
                sb.append(" ").append(cell).append(" |");
            }
            sb.append("\n");
        }

        return sb.toString().trim();
    }
}
