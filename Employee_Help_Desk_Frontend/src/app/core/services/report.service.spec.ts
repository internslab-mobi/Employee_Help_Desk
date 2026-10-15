import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReportService } from './report.service';
import { environment } from '../../../environments/environment';

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ReportService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call Core Backend GET /api/reports/summary with query params for exportSummaryReport', () => {
    const mockBlob = new Blob(['mock excel content'], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    service.exportSummaryReport({
      reportType: 'TICKET_SUMMARY',
      fromDate: '2026-10-01',
      toDate: '2026-10-12'
    }).subscribe(response => {
      expect(response.body).toBeTruthy();
    });

    const expectedBaseUrl = `${environment.backendUrl}/api/reports/summary`;
    const req = httpMock.expectOne(request => request.url === expectedBaseUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('reportType')).toBe('TICKET_SUMMARY');
    expect(req.request.params.get('format')).toBe('EXCEL');
    expect(req.request.params.get('fromDate')).toBe('2026-10-01');
    expect(req.request.params.get('toDate')).toBe('2026-10-12');
    expect(req.request.responseType).toBe('blob');

    req.flush(mockBlob, {
      headers: {
        'Content-Disposition': 'attachment; filename="TICKET_SUMMARY_2026-10-12.xlsx"'
      }
    });
  });

  it('should call Core Backend GET /api/reports/summary with query params for getSummaryReport', () => {
    const mockJson = {
      reportType: 'GENERAL_SUMMARY' as const,
      format: 'JSON' as const,
      generatedAt: '2026-10-12T10:00:00Z',
      role: 'EMPLOYEE',
      employeeId: 10,
      employeeName: 'Jane Doe',
      totalRecords: 5
    };

    service.getSummaryReport({
      reportType: 'GENERAL_SUMMARY',
      fromDate: '2026-10-01',
      toDate: '2026-10-12'
    }).subscribe(data => {
      expect(data).toEqual(mockJson);
      expect(data.totalRecords).toBe(5);
    });

    const expectedBaseUrl = `${environment.backendUrl}/api/reports/summary`;
    const req = httpMock.expectOne(request => request.url === expectedBaseUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('reportType')).toBe('GENERAL_SUMMARY');
    expect(req.request.params.get('format')).toBe('JSON');
    expect(req.request.params.get('fromDate')).toBe('2026-10-01');
    expect(req.request.params.get('toDate')).toBe('2026-10-12');

    req.flush(mockJson);
  });

  it('should pass all supported filters as query parameters when provided', () => {
    service.getSummaryReport({
      reportType: 'ADMIN_SUMMARY',
      format: 'JSON',
      fromDate: '2026-10-01',
      toDate: '2026-10-12',
      departmentId: 5,
      status: 'OPEN',
      priority: 'HIGH',
      employeeId: 99
    }).subscribe();

    const expectedBaseUrl = `${environment.backendUrl}/api/reports/summary`;
    const req = httpMock.expectOne(request => request.url === expectedBaseUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('reportType')).toBe('ADMIN_SUMMARY');
    expect(req.request.params.get('format')).toBe('JSON');
    expect(req.request.params.get('fromDate')).toBe('2026-10-01');
    expect(req.request.params.get('toDate')).toBe('2026-10-12');
    expect(req.request.params.get('departmentId')).toBe('5');
    expect(req.request.params.get('status')).toBe('OPEN');
    expect(req.request.params.get('priority')).toBe('HIGH');
    expect(req.request.params.get('employeeId')).toBe('99');

    req.flush({ totalRecords: 0 });
  });

  it('should omit undefined, null, and empty optional query parameters', () => {
    service.getSummaryReport({
      reportType: 'EMPLOYEE_SUMMARY',
      fromDate: '',
      toDate: '   ',
      departmentId: undefined,
      status: undefined,
      priority: '',
      employeeId: undefined
    }).subscribe();

    const expectedBaseUrl = `${environment.backendUrl}/api/reports/summary`;
    const req = httpMock.expectOne(request => request.url === expectedBaseUrl);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('reportType')).toBe('EMPLOYEE_SUMMARY');
    expect(req.request.params.get('format')).toBe('JSON');
    expect(req.request.params.has('fromDate')).toBe(false);
    expect(req.request.params.has('toDate')).toBe(false);
    expect(req.request.params.has('departmentId')).toBe(false);
    expect(req.request.params.has('status')).toBe(false);
    expect(req.request.params.has('priority')).toBe(false);
    expect(req.request.params.has('employeeId')).toBe(false);

    req.flush({ totalRecords: 0 });
  });

  it('should never call port 8083 directly from Angular ReportService', () => {
    service.getSummaryReport({ reportType: 'GENERAL_SUMMARY' }).subscribe();

    const req = httpMock.expectOne(request => request.url.startsWith(environment.backendUrl));
    expect(req.request.url).not.toContain(':8083');
    req.flush({ totalRecords: 0 });
  });
});
