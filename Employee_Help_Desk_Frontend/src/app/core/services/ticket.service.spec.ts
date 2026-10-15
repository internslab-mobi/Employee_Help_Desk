import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TicketService } from './ticket.service';
import { environment } from '../../../environments/environment';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TicketService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch departments from /departments', () => {
    const mockDepts = [{ id: 1, code: 'IT', name: 'Information Technology' }];
    service.getDepartments().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].name).toBe('Information Technology');
    });

    const req = httpMock.expectOne(`${environment.backendUrl}/departments`);
    expect(req.request.method).toBe('GET');
    req.flush(mockDepts);
  });

  it('should fetch categories from /departments/:id/categories', () => {
    const mockCategories = [{ id: 10, departmentId: 1, name: 'Hardware' }];
    service.getCategories(1).subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].name).toBe('Hardware');
    });

    const req = httpMock.expectOne(`${environment.backendUrl}/departments/1/categories`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCategories);
  });

  it('should search tickets from /tickets/search with query parameters', () => {
    const mockPage = {
      content: [{ id: 101, ticketNumber: 'TCK-101', subject: 'Test', priority: 'HIGH', status: 'OPEN', createdAt: '2026-10-14' }],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: false
    };

    service.searchTickets({ view: 'CREATED', status: 'OPEN', page: 0, size: 10 }).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].ticketNumber).toBe('TCK-101');
    });

    const req = httpMock.expectOne((r) => r.url === `${environment.backendUrl}/tickets/search`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('view')).toBe('CREATED');
    expect(req.request.params.get('status')).toBe('OPEN');
    req.flush(mockPage);
  });

  it('should fetch ticket history, sla, attachments, and messages', () => {
    service.getHistory(101).subscribe((res) => expect(res.length).toBe(0));
    httpMock.expectOne(`${environment.backendUrl}/tickets/101/history`).flush([]);

    service.getSla(101).subscribe((res) => expect(res.status).toBe('ACTIVE'));
    httpMock.expectOne(`${environment.backendUrl}/tickets/101/sla`).flush({ id: 1, status: 'ACTIVE' });

    service.getAttachments(101).subscribe((res) => expect(res.length).toBe(0));
    httpMock.expectOne(`${environment.backendUrl}/tickets/101/attachments`).flush([]);

    service.getMessages(101).subscribe((res) => expect(res.content.length).toBe(0));
    httpMock.expectOne((r) => r.url === `${environment.backendUrl}/tickets/101/messages`).flush({ content: [] });
  });
});
