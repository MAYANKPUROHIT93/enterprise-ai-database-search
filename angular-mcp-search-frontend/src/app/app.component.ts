import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { McpSearchService } from './services/mcp-search.service';
import { SearchResponse, HealthStatus, DatabaseSchema } from './models/mcp.models';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  searchQuery: string = 'Show top 5 highest paid employees with their department name and location';
  isLoading: boolean = false;
  activeResponse: SearchResponse | null = null;
  activeTab: 'insights' | 'table' | 'sql' | 'json' = 'insights';
  healthStatus: HealthStatus | null = null;
  databaseSchema: DatabaseSchema | null = null;
  isSchemaDrawerOpen: boolean = false;
  isSqlMode: boolean = false;
  copyFeedback: boolean = false;
  
  queryHistory: string[] = [
    'Show top 5 highest paid employees with their department name and location',
    'Average salary and headcount per department',
    'List all departments and their budget in descending order'
  ];

  suggestedQueries: string[] = [
    'Show top 5 highest paid employees with their department name and location',
    'Average salary and headcount per department',
    'Which department has the highest budget and what is its location?',
    'List all employees based in the Bengaluru Campus',
    'Show all employees who are currently on leave',
    'Find employees earning more than $100,000 with job title'
  ];

  constructor(
    private mcpService: McpSearchService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.refreshHealth();
    this.loadSchema();
    // Auto-run default query to immediately showcase results
    this.onSearch();
  }

  refreshHealth(): void {
    this.mcpService.getHealth().subscribe(status => {
      this.healthStatus = status;
    });
  }

  loadSchema(): void {
    this.mcpService.getSchema().subscribe(schema => {
      this.databaseSchema = schema;
    });
  }

  onSearch(overrideQuery?: string): void {
    const q = overrideQuery || this.searchQuery;
    if (!q || !q.trim()) return;

    this.searchQuery = q.trim();
    this.isLoading = true;

    // Add to history if unique
    if (!this.queryHistory.includes(this.searchQuery)) {
      this.queryHistory.unshift(this.searchQuery);
      if (this.queryHistory.length > 8) this.queryHistory.pop();
    }

    this.mcpService.search(this.searchQuery).subscribe({
      next: (res) => {
        this.activeResponse = res;
        this.isLoading = false;
        // Automatically switch to table if insights are short or table has many rows
        if (res.rows && res.rows.length > 0 && !res.answer) {
          this.activeTab = 'table';
        } else {
          this.activeTab = 'insights';
        }
      },
      error: (err) => {
        this.activeResponse = {
          query: this.searchQuery,
          success: false,
          error: err.message || 'Error occurred while querying Spring AI orchestrator.'
        };
        this.isLoading = false;
      }
    });
  }

  setQueryAndRun(query: string): void {
    this.searchQuery = query;
    this.onSearch(query);
  }

  copySql(): void {
    if (!this.activeResponse?.sql) return;
    navigator.clipboard.writeText(this.activeResponse.sql).then(() => {
      this.copyFeedback = true;
      setTimeout(() => this.copyFeedback = false, 2000);
    });
  }

  exportCsv(): void {
    if (!this.activeResponse?.rows || this.activeResponse.rows.length === 0) return;
    const columns = this.activeResponse.columns || Object.keys(this.activeResponse.rows[0]);
    
    let csv = columns.join(',') + '\n';
    for (const row of this.activeResponse.rows) {
      const line = columns.map(c => {
        let val = row[c] ?? '';
        val = String(val).replace(/"/g, '""');
        return `"${val}"`;
      }).join(',');
      csv += line + '\n';
    }

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `mcp_query_export_${Date.now()}.csv`;
    link.click();
  }

  exportJson(): void {
    if (!this.activeResponse?.rows) return;
    const jsonStr = JSON.stringify(this.activeResponse.rows, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `mcp_query_export_${Date.now()}.json`;
    link.click();
  }

  toggleSchema(): void {
    this.isSchemaDrawerOpen = !this.isSchemaDrawerOpen;
  }

  isCurrency(col: string, val: any): boolean {
    const c = col.toLowerCase();
    return (c.includes('salary') || c.includes('budget') || c.includes('amount')) && typeof val === 'number';
  }

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(val);
  }

  getCellValue(row: any, col: string): string {
    if (!row) return '—';
    const v = row[col];
    return (v !== null && v !== undefined) ? String(v) : '—';
  }

  getStatusClass(status: string): string {
    const s = String(status).toUpperCase();
    if (s === 'ACTIVE') return 'status-active';
    if (s === 'ON_LEAVE') return 'status-leave';
    if (s === 'TERMINATED') return 'status-terminated';
    return 'status-default';
  }

  getFormattedMarkdown(text: string | undefined): SafeHtml {
    if (!text) return '';

    // 1. Normalize line breaks
    let md = text.replace(/\r\n/g, '\n');

    // 2. Fix inline concatenated bullet items (e.g. "...available: * **id**: ... * **name**: ...")
    md = md.replace(/([^\n])\s*\*\s+\*\*/g, '$1\n* **');
    md = md.replace(/([^\n])\s*\*\s+([A-Za-z0-9])/g, '$1\n* $2');

    // 3. Escape raw HTML tags
    md = md.replace(/&(?!amp;|lt;|gt;|#\d+;)/g, '&amp;')
           .replace(/</g, '&lt;')
           .replace(/>/g, '&gt;');

    // 4. Headers
    md = md.replace(/^### (.*$)/gim, '<h4 class="md-heading">$1</h4>');
    md = md.replace(/^## (.*$)/gim, '<h3 class="md-heading">$1</h3>');
    md = md.replace(/^# (.*$)/gim, '<h2 class="md-heading">$1</h2>');

    // 5. Bold & Italic
    md = md.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    md = md.replace(/\*(.*?)\*/g, '<em>$1</em>');

    // 6. Inline code
    md = md.replace(/`([^`]+)`/g, '<code class="md-inline-code">$1</code>');

    // 7. Structured lists
    const lines = md.split('\n');
    let inList = false;
    let inNumberedList = false;
    const output: string[] = [];

    for (const rawLine of lines) {
      const line = rawLine.trim();
      const bulletMatch = line.match(/^[\*\-]\s+(.*)$/);
      const numberMatch = line.match(/^(\d+)\.\s+(.*)$/);

      if (bulletMatch) {
        if (!inList) {
          if (inNumberedList) { output.push('</ol>'); inNumberedList = false; }
          output.push('<ul class="md-bullet-list">');
          inList = true;
        }
        output.push(`<li><span class="bullet-bullet"></span><div class="bullet-content">${bulletMatch[1]}</div></li>`);
      } else if (numberMatch) {
        if (!inNumberedList) {
          if (inList) { output.push('</ul>'); inList = false; }
          output.push('<ol class="md-numbered-list">');
          inNumberedList = true;
        }
        output.push(`<li><span class="num-bullet">${numberMatch[1]}.</span><div class="bullet-content">${numberMatch[2]}</div></li>`);
      } else {
        if (inList) { output.push('</ul>'); inList = false; }
        if (inNumberedList) { output.push('</ol>'); inNumberedList = false; }

        if (line.length > 0) {
          if (line.startsWith('<h')) {
            output.push(line);
          } else {
            output.push(`<p class="md-paragraph">${line}</p>`);
          }
        }
      }
    }

    if (inList) output.push('</ul>');
    if (inNumberedList) output.push('</ol>');

    return this.sanitizer.bypassSecurityTrustHtml(output.join('\n'));
  }
}

