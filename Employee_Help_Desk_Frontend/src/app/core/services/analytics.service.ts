import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardAnalyticsResponse } from '../models/dashboard.models';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly baseUrl = `${environment.backendUrl}/analytics`;

  constructor(private http: HttpClient) {}

  getDashboardAnalytics(
    fromDate?: string,
    toDate?: string,
    departmentId?: number
  ): Observable<DashboardAnalyticsResponse> {
    let params = new HttpParams();
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    if (departmentId != null) params = params.set('departmentId', departmentId.toString());

    return this.http.get<DashboardAnalyticsResponse>(`${this.baseUrl}/dashboard`, { params });
  }
}
