import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private readonly baseUrl = `${environment.reportServiceUrl}/api/reports`;

  constructor(private http: HttpClient) {}

  exportSummaryReport(
    fromDate?: string,
    toDate?: string,
    departmentId?: number
  ): Observable<HttpResponse<Blob>> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (departmentId != null) params = params.set('departmentId', departmentId.toString());

    return this.http.get(`${this.baseUrl}/summary/export`, {
      params,
      responseType: 'blob',
      observe: 'response'
    });
  }

  downloadBlob(blob: Blob, defaultFilename: string, headerDisposition?: string | null): void {
    let filename = defaultFilename;
    if (headerDisposition) {
      const match = headerDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
      if (match && match[1]) {
        filename = match[1].replace(/['"]/g, '').trim();
      }
    }

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}
