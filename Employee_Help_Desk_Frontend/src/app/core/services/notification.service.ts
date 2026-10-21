import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  NotificationItem,
  NotificationPageResponse,
  UnreadNotificationCountResponse
} from '../models/notification.models';
import { API_ENDPOINTS } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  // private readonly baseUrl = `${environment.backendUrl}/helpdesk/notifications`;
  private readonly baseUrl = API_ENDPOINTS.notifications.base;

  constructor(private http: HttpClient) {}

  getNotifications(page = 0, size = 10): Observable<NotificationPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<NotificationPageResponse>(this.baseUrl, { params });
  }

  getUnreadNotifications(page = 0, size = 10): Observable<NotificationPageResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<NotificationPageResponse>(`${this.baseUrl}/unread`, { params });
  }

  getUnreadCount(): Observable<UnreadNotificationCountResponse> {
    return this.http.get<UnreadNotificationCountResponse>(`${this.baseUrl}/unread/count`);
  }

  markAsRead(notificationId: number): Observable<NotificationItem> {
    return this.http.patch<NotificationItem>(`${this.baseUrl}/${notificationId}/read`, {});
  }
}
