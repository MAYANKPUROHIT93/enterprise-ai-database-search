export interface SearchRequest {
  query: string;
}

export interface SearchResponse {
  query: string;
  answer?: string;
  sql?: string;
  markdown?: string;
  columns?: string[];
  rows?: Record<string, any>[];
  rowCount?: number;
  executionTimeMs?: number;
  model?: string;
  mcpServer?: string;
  success: boolean;
  error?: string;
}

export interface HealthStatus {
  status: string;
  service: string;
  geminiModel: string;
  mcpServerPing?: {
    status: string;
    host?: string;
    latencyMs?: number;
    mysqlVersion?: string;
    currentUser?: string;
    readOnly?: boolean;
    error?: string;
  };
}

export interface TableColumn {
  column_name: string;
  data_type: string;
  column_type: string;
  nullable: boolean;
  key?: string;
  default_value?: string;
  comment?: string;
}

export interface DatabaseSchema {
  database: string;
  tables?: Array<{
    table_name: string;
    table_type: string;
    approx_rows: number;
    engine: string;
  }>;
  employeeColumns?: TableColumn[];
  departmentColumns?: TableColumn[];
}
