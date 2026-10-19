import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssignableAgentResponse,
  CategoryResponse,
  CreateTicketRequest,
  DepartmentResponse,
  PageResponse,
  SubCategoryResponse,
  TicketAttachmentResponse,
  TicketHistoryResponse,
  TicketMessageResponse,
  TicketResponse,
  TicketSearchParams,
  TicketSlaResponse
} from '../models/ticket.models';
import { API_ENDPOINTS } from '../config/api.config';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  // private readonly baseUrl = `${environment.backendUrl}/helpdesk/tickets`;
  // private readonly rootUrl = environment.backendUrl;
  private readonly baseUrl = API_ENDPOINTS.tickets.base;

  constructor(private http: HttpClient) {}

  getDepartments(): Observable<DepartmentResponse[]> {
    return this.http.get<DepartmentResponse[]>(API_ENDPOINTS.departments.base);
  }

  getCategories(departmentId: number): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(`${API_ENDPOINTS.categories.base}?departmentId=${departmentId}`);
  }

  getSubCategories(categoryId: number): Observable<SubCategoryResponse[]> {
    return this.http.get<SubCategoryResponse[]>(`${API_ENDPOINTS.subCategories.base}?categoryId=${categoryId}`);
  }

  createTicket(request: CreateTicketRequest, attachments?: File[]): Observable<any> {
    const formData = new FormData();
    const jsonBlob = new Blob([JSON.stringify(request)], { type: 'application/json' });
    formData.append('request', jsonBlob);

    if (attachments && attachments.length > 0) {
      for (const file of attachments) {
        formData.append('attachments', file, file.name);
      }
    }

    return this.http.post<any>(this.baseUrl, formData);
  }

  getTicket(ticketId: number): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.baseUrl}/${ticketId}`);
  }

  searchTickets(params: TicketSearchParams): Observable<PageResponse<TicketResponse>> {
    let httpParams = new HttpParams().set('view', params.view);

    if (params.employeeId !== undefined && params.employeeId !== null) {
      httpParams = httpParams.set('employeeId', params.employeeId.toString());
    }
    if (params.departmentId !== undefined && params.departmentId !== null) {
      httpParams = httpParams.set('departmentId', params.departmentId.toString());
    }
    if (params.agentId !== undefined && params.agentId !== null) {
      httpParams = httpParams.set('agentId', params.agentId.toString());
    }
    if (params.status && params.status !== 'ALL') {
      httpParams = httpParams.set('status', params.status);
    }
    if (params.priority && params.priority !== 'ALL') {
      httpParams = httpParams.set('priority', params.priority);
    }
    if (params.fromDate) {
      httpParams = httpParams.set('fromDate', params.fromDate);
    }
    if (params.toDate) {
      httpParams = httpParams.set('toDate', params.toDate);
    }
    if (params.search && params.search.trim() !== '') {
      httpParams = httpParams.set('search', params.search.trim());
    }
    if (params.unassigned !== undefined && params.unassigned !== null) {
      httpParams = httpParams.set('unassigned', String(params.unassigned));
    }
    if (params.slaStatus && params.slaStatus !== 'ALL') {
      httpParams = httpParams.set('slaStatus', params.slaStatus);
    }
    if (params.page !== undefined && params.page !== null) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params.size !== undefined && params.size !== null) {
      httpParams = httpParams.set('size', params.size.toString());
    }
    if (params.sort) {
      httpParams = httpParams.set('sort', params.sort);
    }

    return this.http.get<PageResponse<TicketResponse>>(`${this.baseUrl}/search`, { params: httpParams });
  }

  getMessages(ticketId: number, page = 0, size = 50): Observable<PageResponse<TicketMessageResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<TicketMessageResponse>>(`${this.baseUrl}/${ticketId}/messages`, { params });
  }

  addMessage(ticketId: number, content: string, attachments?: File[]): Observable<any> {
    if (attachments && attachments.length > 0) {
      const formData = new FormData();
      formData.append('content', content);
      for (const file of attachments) {
        formData.append('attachments', file, file.name);
      }
      return this.http.post<any>(`${this.baseUrl}/${ticketId}/messages`, formData);
    } else {
      return this.http.post<any>(`${this.baseUrl}/${ticketId}/messages`, { content });
    }
  }

  getHistory(ticketId: number): Observable<TicketHistoryResponse[]> {
    return this.http.get<TicketHistoryResponse[]>(`${this.baseUrl}/${ticketId}/history`);
  }

  getSla(ticketId: number): Observable<TicketSlaResponse> {
    return this.http.get<TicketSlaResponse>(`${this.baseUrl}/${ticketId}/sla`);
  }

  getAttachments(ticketId: number): Observable<TicketAttachmentResponse[]> {
    return this.http.get<TicketAttachmentResponse[]>(`${this.baseUrl}/${ticketId}/attachments`);
  }

  downloadAttachment(ticketId: number, attachmentId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.baseUrl}/${ticketId}/attachments/${attachmentId}/download`, {
      responseType: 'blob',
      observe: 'response'
    });
  }

  updateTicket(ticketId: number, patch: { status: string; reason?: string; resolution?: string; agentId?: number }): Observable<any> {
    return this.http.patch<any>(`${this.baseUrl}/${ticketId}`, patch);
  }

  getAssignableAgents(ticketId: number): Observable<AssignableAgentResponse[]> {
    return this.http.get<AssignableAgentResponse[]>(`${this.baseUrl}/${ticketId}/assignable-agents`);
  }
}
