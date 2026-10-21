import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { environment } from '../../../environments/environment';
import { NotificationItem, NotificationPageResponse, UnreadNotificationCountResponse } from '../models/notification.models';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch paginated notifications from /notifications', () => {
    const mockPage: NotificationPageResponse = {
      content: [
        {
          id: 1,
          recipientId: 10,
          ticketId: 101,
          ticketNumber: 'TCK-101',
          type: 'TICKET_ASSIGNED',
          title: 'Ticket Assigned',
          message: 'Ticket TCK-101 has been assigned to you',
          read: false,
          createdAt: '2026-10-15T10:00:00Z'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      last: true
    };

    service.getNotifications(0, 10).subscribe((res) => {
      expect(res.content.length).toBe(1);
      expect(res.content[0].title).toBe('Ticket Assigned');
      expect(res.content[0].read).toBe(false);
    });

    const req = httpMock.expectOne((r) => r.url === `${environment.backendUrl}/notifications`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    req.flush(mockPage);
  });

  it('should fetch unread notifications from /notifications/unread', () => {
    const mockPage: NotificationPageResponse = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 10,
      number: 0,
      last: true
    };

    service.getUnreadNotifications(0, 5).subscribe((res) => {
      expect(res.content.length).toBe(0);
    });

    const req = httpMock.expectOne((r) => r.url === `${environment.backendUrl}/notifications/unread`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('5');
    req.flush(mockPage);
  });

  it('should fetch unread count from /notifications/unread/count', () => {
    const mockCount: UnreadNotificationCountResponse = { unreadCount: 3 };

    service.getUnreadCount().subscribe((res) => {
      expect(res.unreadCount).toBe(3);
    });

    const req = httpMock.expectOne(`${environment.backendUrl}/notifications/unread/count`);
    expect(req.request.method).toBe('GET');
    req.flush(mockCount);
  });

  it('should mark notification as read via PATCH /notifications/:id/read', () => {
    const mockItem: NotificationItem = {
      id: 5,
      recipientId: 10,
      type: 'TICKET_STATUS_CHANGED',
      title: 'Ticket Status Updated',
      message: 'Status changed to RESOLVED',
      read: true,
      createdAt: '2026-10-15T11:00:00Z'
    };

    service.markAsRead(5).subscribe((res) => {
      expect(res.read).toBe(true);
      expect(res.id).toBe(5);
    });

    const req = httpMock.expectOne(`${environment.backendUrl}/notifications/5/read`);
    expect(req.request.method).toBe('PATCH');
    req.flush(mockItem);
  });
});
