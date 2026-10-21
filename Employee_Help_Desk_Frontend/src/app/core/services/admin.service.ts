import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_ENDPOINTS } from '../config/api.config';
import { CategoryResponse, DepartmentResponse, PageResponse, SubCategoryResponse } from '../models/ticket.models';
import {
  CreateCategoryRequest,
  CreateDepartmentRequest,
  CreateHolidayRequest,
  CreateSubCategoryRequest,
  CreateUserRequest,
  EmployeeResponse,
  HolidayResponse,
  SkillResponse,
  CreateSkillRequest,
  AssignSkillsRequest,
  SlaPolicyResponse,
  CreateSlaPolicyRequest
} from '../models/admin.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  constructor(private http: HttpClient) {}

  // Departments
  getDepartments(): Observable<DepartmentResponse[]> {
    return this.http.get<DepartmentResponse[]>(API_ENDPOINTS.departments.base);
  }

  createDepartment(payload: CreateDepartmentRequest): Observable<DepartmentResponse> {
    return this.http.post<DepartmentResponse>(API_ENDPOINTS.departments.base, payload);
  }

  // Categories
  getCategories(departmentId?: number): Observable<CategoryResponse[]> {
    let params = new HttpParams();
    if (departmentId !== undefined && departmentId !== null) {
      params = params.set('departmentId', departmentId.toString());
    }
    return this.http.get<CategoryResponse[]>(API_ENDPOINTS.categories.base, { params });
  }

  createCategory(payload: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(API_ENDPOINTS.categories.base, payload);
  }

  // SubCategories
  getSubCategories(categoryId?: number): Observable<SubCategoryResponse[]> {
    let params = new HttpParams();
    if (categoryId !== undefined && categoryId !== null) {
      params = params.set('categoryId', categoryId.toString());
    }
    return this.http.get<SubCategoryResponse[]>(API_ENDPOINTS.subCategories.base, { params });
  }

  createSubCategory(payload: CreateSubCategoryRequest): Observable<SubCategoryResponse> {
    return this.http.post<SubCategoryResponse>(API_ENDPOINTS.subCategories.base, payload);
  }

  // Holidays
  getHolidays(departmentId?: number, year?: number): Observable<HolidayResponse[]> {
    let params = new HttpParams();
    if (departmentId !== undefined && departmentId !== null) {
      params = params.set('departmentId', departmentId.toString());
    }
    if (year !== undefined && year !== null) {
      params = params.set('year', year.toString());
    }
    return this.http.get<HolidayResponse[]>(API_ENDPOINTS.holidays.base, { params });
  }

  createHoliday(payload: CreateHolidayRequest): Observable<HolidayResponse> {
    return this.http.post<HolidayResponse>(API_ENDPOINTS.holidays.base, payload);
  }

  deleteHoliday(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${API_ENDPOINTS.holidays.base}/${id}`);
  }

  // User Management
  createUser(payload: CreateUserRequest): Observable<EmployeeResponse> {
    return this.http.post<EmployeeResponse>(API_ENDPOINTS.user.base, payload);
  }

  getManagers(departmentId?: number): Observable<PageResponse<EmployeeResponse>> {
    let params = new HttpParams()
      .set('search', '')
      .set('role', 'MANAGER')
      .set('size', '100');
    if (departmentId !== undefined && departmentId !== null) {
      params = params.set('departmentId', departmentId.toString());
    }
    return this.http.get<PageResponse<EmployeeResponse>>(API_ENDPOINTS.user.search, { params });
  }

  getAgents(departmentId?: number): Observable<PageResponse<EmployeeResponse>> {
    let params = new HttpParams()
      .set('search', '')
      .set('role', 'AGENT')
      .set('size', '100');
    if (departmentId !== undefined && departmentId !== null) {
      params = params.set('departmentId', departmentId.toString());
    }
    return this.http.get<PageResponse<EmployeeResponse>>(API_ENDPOINTS.user.search, { params });
  }

  getEmployees(paramsObj: {
    search?: string;
    departmentId?: number;
    role?: string;
    status?: string;
    page?: number;
    size?: number;
  }): Observable<PageResponse<EmployeeResponse>> {
    let params = new HttpParams();
    params = params.set('search', paramsObj.search || '');
    if (paramsObj.departmentId !== undefined && paramsObj.departmentId !== null) {
      params = params.set('departmentId', paramsObj.departmentId.toString());
    }
    if (paramsObj.role && paramsObj.role !== 'ALL') {
      params = params.set('role', paramsObj.role);
    }
    if (paramsObj.status && paramsObj.status !== 'ALL') {
      params = params.set('status', paramsObj.status);
    }
    if (paramsObj.page !== undefined) {
      params = params.set('page', paramsObj.page.toString());
    }
    params = params.set('size', (paramsObj.size || 20).toString());

    return this.http.get<PageResponse<EmployeeResponse>>(API_ENDPOINTS.user.search, { params });
  }

  // Skills
  getSkills(): Observable<SkillResponse[]> {
    return this.http.get<SkillResponse[]>(API_ENDPOINTS.skills.base);
  }

  createSkill(payload: CreateSkillRequest): Observable<SkillResponse> {
    return this.http.post<SkillResponse>(API_ENDPOINTS.skills.base, payload);
  }

  getAgentSkills(agentId: number): Observable<SkillResponse[]> {
    return this.http.get<SkillResponse[]>(API_ENDPOINTS.skills.agent(agentId));
  }

  assignAgentSkills(agentId: number, payload: AssignSkillsRequest): Observable<SkillResponse[]> {
    return this.http.post<SkillResponse[]>(API_ENDPOINTS.skills.agent(agentId), payload);
  }

  getSubCategorySkills(subCategoryId: number): Observable<SkillResponse[]> {
    return this.http.get<SkillResponse[]>(API_ENDPOINTS.skills.subCategory(subCategoryId));
  }

  assignSubCategorySkills(subCategoryId: number, payload: AssignSkillsRequest): Observable<SkillResponse[]> {
    return this.http.post<SkillResponse[]>(API_ENDPOINTS.skills.subCategory(subCategoryId), payload);
  }

  // SLA Policies
  getSlaPolicies(departmentId?: number): Observable<SlaPolicyResponse[]> {
    let params = new HttpParams();
    if (departmentId !== undefined && departmentId !== null) {
      params = params.set('departmentId', departmentId.toString());
    }
    return this.http.get<SlaPolicyResponse[]>(API_ENDPOINTS.slaPolicies.base, { params });
  }

  createSlaPolicy(payload: CreateSlaPolicyRequest): Observable<SlaPolicyResponse> {
    return this.http.post<SlaPolicyResponse>(API_ENDPOINTS.slaPolicies.base, payload);
  }
}
