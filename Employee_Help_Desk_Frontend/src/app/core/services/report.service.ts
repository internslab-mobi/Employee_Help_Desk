import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
<<<<<<< HEAD
import { API_ENDPOINTS } from '../config/api.config';
import { ReportSummaryRequest, ReportSummaryResponse, ReportType } from '../models/report.models';
=======
import { environment } from '../../../environments/environment';
>>>>>>> 22515536 (microservices summary report)

@Injectable({
  providedIn: 'root'
})
export class ReportService {
<<<<<<< HEAD
  // Integrates strictly through the Core Backend - never calls Report Microservice directly
  private readonly baseUrl = API_ENDPOINTS.reports.base;

  constructor(private http: HttpClient) {}

  /**
   * Request an Excel summary export through the Core Backend GET /api/reports/summary.
   * Supports both object-based requests and legacy parameter lists.
   */
  exportSummaryReport(
    reqOrFromDate?: ReportSummaryRequest | string,
    toDate?: string,
    departmentId?: number,
    reportType?: ReportType
  ): Observable<HttpResponse<Blob>> {
    let request: ReportSummaryRequest;

    if (typeof reqOrFromDate === 'object' && reqOrFromDate !== null) {
      request = {
        ...reqOrFromDate,
        format: 'EXCEL'
      };
    } else {
      request = {
        format: 'EXCEL',
        reportType: reportType || 'GENERAL_SUMMARY',
        fromDate: reqOrFromDate as string | undefined,
        toDate: toDate,
        departmentId: departmentId
      };
    }

    const params = this.buildHttpParams(request);

    return this.http.get(`${this.baseUrl}/summary`, {
=======
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
>>>>>>> 22515536 (microservices summary report)
      params,
      responseType: 'blob',
      observe: 'response'
    });
  }

<<<<<<< HEAD
  /**
   * Request an on-screen JSON summary report through the Core Backend GET /api/reports/summary.
   */
  getSummaryReport(request: ReportSummaryRequest): Observable<ReportSummaryResponse> {
    const payload: ReportSummaryRequest = {
      ...request,
      format: 'JSON'
    };

    const params = this.buildHttpParams(payload);

    return this.http.get<ReportSummaryResponse>(`${this.baseUrl}/summary`, {
      params
    });
  }

  /**
   * Safely constructs HttpParams from a ReportSummaryRequest,
   * omitting undefined, null, or empty string values.
   */
  private buildHttpParams(request: ReportSummaryRequest): HttpParams {
    let params = new HttpParams();

    if (request.reportType) {
      params = params.set('reportType', request.reportType);
    }
    if (request.format) {
      params = params.set('format', request.format);
    }
    if (request.fromDate && request.fromDate.trim() !== '') {
      params = params.set('fromDate', request.fromDate.trim());
    }
    if (request.toDate && request.toDate.trim() !== '') {
      params = params.set('toDate', request.toDate.trim());
    }
    if (request.departmentId !== undefined && request.departmentId !== null) {
      params = params.set('departmentId', request.departmentId.toString());
    }
    if (request.status && request.status.trim() !== '') {
      params = params.set('status', request.status.trim());
    }
    if (request.priority && request.priority.trim() !== '') {
      params = params.set('priority', request.priority.trim());
    }
    if (request.employeeId !== undefined && request.employeeId !== null) {
      params = params.set('employeeId', request.employeeId.toString());
    }

    return params;
  }

  /**
   * Trigger browser file download from an HTTP Blob response.
   */
=======
>>>>>>> 22515536 (microservices summary report)
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
