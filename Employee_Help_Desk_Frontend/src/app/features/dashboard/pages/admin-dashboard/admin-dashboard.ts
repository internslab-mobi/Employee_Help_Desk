import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalyticsService } from '../../../../core/services/analytics.service';
import { TicketService } from '../../../../core/services/ticket.service';
import { AdminService } from '../../../../core/services/admin.service';
import { DashboardAnalyticsResponse, RecentTicket } from '../../../../core/models/dashboard.models';
import {
  CategoryResponse,
  DepartmentResponse,
  PageResponse,
  SubCategoryResponse,
  TicketResponse
} from '../../../../core/models/ticket.models';
import {
  AssignSkillsRequest,
  CreateCategoryRequest,
  CreateDepartmentRequest,
  CreateHolidayRequest,
  CreateSkillRequest,
  CreateSlaPolicyRequest,
  CreateSubCategoryRequest,
  CreateUserRequest,
  EmployeeResponse,
  HolidayResponse,
  SkillResponse,
  SlaPolicyResponse
} from '../../../../core/models/admin.models';
import { DashboardStateService } from '../../services/dashboard-state.service';
import { AnalyticsDashboardView } from '../../dashboard';
import { DashboardSidebarComponent } from '../../components/sidebar/dashboard-sidebar';
import { DashboardHeaderComponent } from '../../components/header/dashboard-header';
import { NotificationDrawerComponent } from '../../components/notification-drawer/notification-drawer';
import { TicketPreviewComponent } from '../../components/ticket-preview/ticket-preview';
import { ReportSummaryModalComponent } from '../../components/report-summary-modal/report-summary-modal';
import { UserRole } from '../../../../core/models/auth.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DashboardSidebarComponent,
    DashboardHeaderComponent,
    NotificationDrawerComponent,
    TicketPreviewComponent,
    ReportSummaryModalComponent
  ],
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css']
})
export class AdminDashboardComponent implements OnInit {
  readonly state = inject(DashboardStateService);
  private readonly analyticsService = inject(AnalyticsService);
  private readonly ticketService = inject(TicketService);
  private readonly adminService = inject(AdminService);

  activeNav = signal<string>('dashboard');
  selectedAnalyticsView = signal<AnalyticsDashboardView>('OVERALL_OVERVIEW');

  analytics = signal<DashboardAnalyticsResponse | null>(null);
  isLoading = signal<boolean>(true);
  errorMessage = signal<string | null>(null);

  // Lookups
  departmentsList = signal<DepartmentResponse[]>([]);
  agentsList = signal<EmployeeResponse[]>([]);

  // ===================== ALL TICKETS ORG-WIDE =====================
  serverTickets = signal<TicketResponse[]>([]);
  isLoadingTickets = signal<boolean>(false);
  ticketCurrentPage = signal<number>(0);
  ticketPageSize = signal<number>(10);
  ticketTotalPages = signal<number>(0);
  ticketTotalElements = signal<number>(0);

  // Comprehensive Filters
  selectedDepartmentId = signal<number | null>(null);
  selectedAgentId = signal<number | null>(null);
  ticketStatusFilter = signal<string>('ALL');
  ticketPriorityFilter = signal<string>('ALL');
  ticketSlaFilter = signal<string>('ALL');
  ticketSearchTerm = signal<string>('');
  selectedDateRange = signal<string>('all');
  customFromDate = signal<string>('');
  customToDate = signal<string>('');
  showFilters = signal<boolean>(false);
  showAnalyticsSection = signal<boolean>(true);

  readonly displayTickets = computed<TicketResponse[]>(() => this.serverTickets());

  // ===================== BREACHED TICKETS (ORG-WIDE MONITORING) =====================
  breachedTickets = signal<TicketResponse[]>([]);
  isLoadingBreachedTickets = signal<boolean>(false);

  // ===================== USER MANAGEMENT =====================
  usersList = signal<EmployeeResponse[]>([]);
  isLoadingUsers = signal<boolean>(false);
  userSearchTerm = signal<string>('');
  userRoleFilter = signal<string>('ALL');
  userDeptFilter = signal<number | null>(null);
  userCurrentPage = signal<number>(0);
  userPageSize = signal<number>(10);
  userTotalPages = signal<number>(0);
  userTotalElements = signal<number>(0);

  showCreateUserModal = signal<boolean>(false);
  userFirstName = signal<string>('');
  userLastName = signal<string>('');
  userEmail = signal<string>('');
  userPhone = signal<string>('');
  userDesignation = signal<string>('');
  userRole = signal<UserRole>('EMPLOYEE');
  userDepartmentId = signal<number | null>(null);
  userManagerId = signal<number | null>(null);
  userDateOfJoining = signal<string>('');
  userTimezone = signal<string>('Asia/Kolkata');
  isSubmittingUser = signal<boolean>(false);
  availableManagers = signal<EmployeeResponse[]>([]);

  // ===================== CONFIGURATION =====================
  activeConfigTab = signal<'departments' | 'categories' | 'subcategories' | 'holidays' | 'sla-policies' | 'skills'>('departments');

  // Departments Config
  configDepartmentsList = signal<DepartmentResponse[]>([]);
  showAddDeptModal = signal<boolean>(false);
  newDeptCode = signal<string>('');
  newDeptName = signal<string>('');
  newDeptDesc = signal<string>('');
  newDeptTimezone = signal<string>('Asia/Kolkata');
  isSubmittingDept = signal<boolean>(false);

  // Categories Config
  configCategoriesList = signal<CategoryResponse[]>([]);
  configCategoryDeptFilter = signal<number | null>(null);
  showAddCatModal = signal<boolean>(false);
  newCatDeptId = signal<number | null>(null);
  newCatName = signal<string>('');
  newCatDesc = signal<string>('');
  isSubmittingCat = signal<boolean>(false);

  // SubCategories Config
  configSubCategoriesList = signal<SubCategoryResponse[]>([]);
  configSubCatCategoryFilter = signal<number | null>(null);
  showAddSubCatModal = signal<boolean>(false);
  newSubCatCatId = signal<number | null>(null);
  newSubCatName = signal<string>('');
  newSubCatDesc = signal<string>('');
  newSubCatPriority = signal<string>('MEDIUM');
  selectedSubCatSkillIds = signal<number[]>([]);
  isSubmittingSubCat = signal<boolean>(false);

  // Holiday Calendar Config
  holidaysList = signal<HolidayResponse[]>([]);
  holidayDeptFilter = signal<number | null>(null);
  holidayYearFilter = signal<number>(new Date().getFullYear());
  showAddHolidayModal = signal<boolean>(false);
  newHolidayDeptId = signal<number | null>(null);
  newHolidayDate = signal<string>('');
  newHolidayName = signal<string>('');
  isSubmittingHoliday = signal<boolean>(false);
  isDeletingHoliday = signal<{ [id: number]: boolean }>({});

  // Skills Directory Config
  skillsList = signal<SkillResponse[]>([]);
  isLoadingSkills = signal<boolean>(false);
  showAddSkillModal = signal<boolean>(false);
  newSkillName = signal<string>('');
  newSkillDesc = signal<string>('');
  isSubmittingSkill = signal<boolean>(false);

  // SLA Policies Config
  slaPoliciesList = signal<SlaPolicyResponse[]>([]);
  slaPolicyDeptFilter = signal<number | null>(null);
  isLoadingSla = signal<boolean>(false);
  showAddSlaModal = signal<boolean>(false);
  newSlaDeptId = signal<number | null>(null);
  newSlaSubCatId = signal<number | null>(null);
  newSlaDuration = signal<number>(8);
  newSlaWarning = signal<number>(4);
  isSubmittingSla = signal<boolean>(false);

  // Agent Skills Assignment Modal
  showAgentSkillsModal = signal<boolean>(false);
  selectedAgentForSkills = signal<EmployeeResponse | null>(null);
  selectedAgentSkillIds = signal<number[]>([]);
  isLoadingAgentSkills = signal<boolean>(false);
  isSubmittingAgentSkills = signal<boolean>(false);

  ngOnInit(): void {
    this.state.loadUnreadCount();
    this.loadAllData();
    this.loadDepartments();
    this.loadAgentsList();
  }

  loadAllData(): void {
    this.loadDashboardData();
    this.loadTicketsPage(0);
    this.loadBreachedTickets();
  }

  loadDepartments(): void {
    this.adminService.getDepartments().subscribe({
      next: (depts) => {
        this.departmentsList.set(depts || []);
        this.configDepartmentsList.set(depts || []);
        if (depts && depts.length > 0 && !this.newCatDeptId()) {
          this.newCatDeptId.set(depts[0].id);
        }
        if (depts && depts.length > 0 && !this.newHolidayDeptId()) {
          this.newHolidayDeptId.set(depts[0].id);
        }
      },
      error: () => {}
    });
  }

  loadAgentsList(): void {
    this.adminService.getAgents().subscribe({
      next: (res) => {
        this.agentsList.set(res?.content || []);
      },
      error: () => {}
    });
  }

  setActiveNav(nav: string): void {
    this.state.clearSelectedTicket();
    if (nav === 'tickets' || nav === 'my-tickets') {
      this.activeNav.set('all-tickets');
      this.loadTicketsPage(0);
    } else {
      this.activeNav.set(nav);
      if (nav === 'all-tickets') {
        this.loadTicketsPage(0);
      } else if (nav === 'assign') {
        this.loadBreachedTickets();
      } else if (nav === 'users') {
        this.loadUsersPage(0);
      } else if (nav === 'config') {
        this.loadConfigTab(this.activeConfigTab());
      }
    }
  }

  setAnalyticsView(view: AnalyticsDashboardView): void {
    this.selectedAnalyticsView.set(view);
  }

  toggleAnalyticsSection(): void {
    this.showAnalyticsSection.update((v) => !v);
  }

  toggleFilters(): void {
    this.showFilters.update((v) => !v);
  }

  setDateRange(range: string): void {
    this.selectedDateRange.set(range);
    if (range !== 'custom') {
      this.loadDashboardData();
      this.loadTicketsPage(0);
    }
  }

  applyCustomDateRange(): void {
    if (this.customFromDate() && this.customToDate()) {
      if (this.customFromDate() > this.customToDate()) {
        this.state.showNotification('error', 'Start date cannot be after end date.');
        return;
      }
      this.loadDashboardData();
      this.loadTicketsPage(0);
    }
  }

  onDepartmentChange(deptId: number | null): void {
    this.selectedDepartmentId.set(deptId);
    this.loadDashboardData();
    this.loadTicketsPage(0);
  }

  onAgentFilterChange(agentId: number | null): void {
    this.selectedAgentId.set(agentId);
    this.loadTicketsPage(0);
  }

  onStatusFilterChange(status: string): void {
    this.ticketStatusFilter.set(status);
    this.loadTicketsPage(0);
  }

  onPriorityFilterChange(priority: string): void {
    this.ticketPriorityFilter.set(priority);
    this.loadTicketsPage(0);
  }

  onSlaFilterChange(slaStatus: string): void {
    this.ticketSlaFilter.set(slaStatus);
    this.loadTicketsPage(0);
  }

  resolveDateRange(): { from?: string; to?: string } {
    const range = this.selectedDateRange();
    if (range === 'custom') {
      return { from: this.customFromDate() || undefined, to: this.customToDate() || undefined };
    }
    const today = new Date();
    const formatDate = (d: Date) => d.toISOString().split('T')[0];
    if (range === 'today') {
      const t = formatDate(today);
      return { from: t, to: t };
    }
    if (range === '7days') {
      const from = new Date(today);
      from.setDate(today.getDate() - 7);
      return { from: formatDate(from), to: formatDate(today) };
    }
    if (range === '30days') {
      const from = new Date(today);
      from.setDate(today.getDate() - 30);
      return { from: formatDate(from), to: formatDate(today) };
    }
    if (range === 'thisMonth') {
      const from = new Date(today.getFullYear(), today.getMonth(), 1);
      return { from: formatDate(from), to: formatDate(today) };
    }
    return {};
  }

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    const dateRange = this.resolveDateRange();

    this.analyticsService
      .getDashboardAnalytics(
        dateRange.from,
        dateRange.to,
        this.selectedDepartmentId() ?? undefined
      )
      .subscribe({
        next: (res) => {
          this.isLoading.set(false);
          this.analytics.set(res);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(err?.error?.message || 'Failed to load analytics.');
        }
      });
  }

  // ===================== ALL TICKETS SEARCH & PAGINATION =====================
  loadTicketsPage(page: number): void {
    this.isLoadingTickets.set(true);
    const dateRange = this.resolveDateRange();

    this.ticketService
      .searchTickets({
        view: 'DEPARTMENT',
        departmentId: this.selectedDepartmentId() ?? undefined,
        agentId: this.selectedAgentId() ?? undefined,
        status: this.ticketStatusFilter(),
        priority: this.ticketPriorityFilter(),
        slaStatus: this.ticketSlaFilter(),
        fromDate: dateRange.from,
        toDate: dateRange.to,
        search: this.ticketSearchTerm(),
        page: page,
        size: this.ticketPageSize()
      })
      .subscribe({
        next: (res: PageResponse<TicketResponse>) => {
          this.isLoadingTickets.set(false);
          this.serverTickets.set(res?.content || []);
          this.ticketCurrentPage.set(res?.number ?? page);
          this.ticketTotalPages.set(res?.totalPages ?? 0);
          this.ticketTotalElements.set(res?.totalElements ?? 0);
        },
        error: () => {
          this.isLoadingTickets.set(false);
          this.serverTickets.set([]);
          this.ticketTotalElements.set(0);
        }
      });
  }

  onSearchInput(term: string): void {
    this.ticketSearchTerm.set(term);
    this.loadTicketsPage(0);
  }

  clearSearch(): void {
    this.ticketSearchTerm.set('');
    this.loadTicketsPage(0);
  }

  clearAllFilters(): void {
    this.selectedDepartmentId.set(null);
    this.selectedAgentId.set(null);
    this.ticketStatusFilter.set('ALL');
    this.ticketPriorityFilter.set('ALL');
    this.ticketSlaFilter.set('ALL');
    this.ticketSearchTerm.set('');
    this.selectedDateRange.set('all');
    this.customFromDate.set('');
    this.customToDate.set('');
    this.loadTicketsPage(0);
  }

  prevTicketsPage(): void {
    if (this.ticketCurrentPage() > 0) {
      this.loadTicketsPage(this.ticketCurrentPage() - 1);
    }
  }

  nextTicketsPage(): void {
    if (this.ticketCurrentPage() + 1 < this.ticketTotalPages()) {
      this.loadTicketsPage(this.ticketCurrentPage() + 1);
    }
  }

  navigateToTicketsWithFilter(filter: { status?: string; priority?: string; departmentId?: number }): void {
    this.state.clearSelectedTicket();
    this.activeNav.set('all-tickets');
    if (filter.status) this.ticketStatusFilter.set(filter.status);
    if (filter.priority) this.ticketPriorityFilter.set(filter.priority);
    if (filter.departmentId) this.selectedDepartmentId.set(filter.departmentId);
    this.loadTicketsPage(0);
  }

  selectTicket(ticket: RecentTicket | TicketResponse): void {
    this.state.selectTicket(ticket);
  }

  // ===================== BREACHED TICKETS (ORG-WIDE MONITORING) =====================
  loadBreachedTickets(): void {
    this.isLoadingBreachedTickets.set(true);
    this.ticketService
      .searchTickets({
        view: 'DEPARTMENT',
        slaStatus: 'BREACHED',
        page: 0,
        size: 50
      })
      .subscribe({
        next: (res: PageResponse<TicketResponse>) => {
          this.isLoadingBreachedTickets.set(false);
          this.breachedTickets.set(res?.content || []);
        },
        error: () => {
          this.isLoadingBreachedTickets.set(false);
          this.breachedTickets.set([]);
        }
      });
  }

  // ===================== USER MANAGEMENT =====================
  loadUsersPage(page: number): void {
    this.isLoadingUsers.set(true);
    this.adminService
      .getEmployees({
        search: this.userSearchTerm(),
        departmentId: this.userDeptFilter() ?? undefined,
        role: this.userRoleFilter(),
        page: page,
        size: this.userPageSize()
      })
      .subscribe({
        next: (res) => {
          this.isLoadingUsers.set(false);
          this.usersList.set(res?.content || []);
          this.userCurrentPage.set(res?.number ?? page);
          this.userTotalPages.set(res?.totalPages ?? 0);
          this.userTotalElements.set(res?.totalElements ?? 0);
        },
        error: () => {
          this.isLoadingUsers.set(false);
          this.usersList.set([]);
          this.userTotalElements.set(0);
        }
      });
  }

  onUserSearchInput(term: string): void {
    this.userSearchTerm.set(term);
    this.loadUsersPage(0);
  }

  onUserRoleFilterChange(role: string): void {
    this.userRoleFilter.set(role);
    this.loadUsersPage(0);
  }

  onUserDeptFilterChange(deptId: number | null): void {
    this.userDeptFilter.set(deptId);
    this.loadUsersPage(0);
  }

  prevUsersPage(): void {
    if (this.userCurrentPage() > 0) {
      this.loadUsersPage(this.userCurrentPage() - 1);
    }
  }

  nextUsersPage(): void {
    if (this.userCurrentPage() + 1 < this.userTotalPages()) {
      this.loadUsersPage(this.userCurrentPage() + 1);
    }
  }

  openCreateUserModal(): void {
    this.userFirstName.set('');
    this.userLastName.set('');
    this.userEmail.set('');
    this.userPhone.set('');
    this.userDesignation.set('');
    this.userRole.set('EMPLOYEE');
    const firstDept = this.departmentsList().length ? this.departmentsList()[0].id : null;
    this.userDepartmentId.set(firstDept);
    this.userManagerId.set(null);
    this.userDateOfJoining.set(new Date().toISOString().split('T')[0]);
    this.userTimezone.set('Asia/Kolkata');
    this.showCreateUserModal.set(true);
    if (firstDept) {
      this.loadManagersForDept(firstDept);
    }
  }

  closeCreateUserModal(): void {
    this.showCreateUserModal.set(false);
  }

  onUserDeptSelect(deptId: number): void {
    this.userDepartmentId.set(deptId);
    this.userManagerId.set(null);
    this.loadManagersForDept(deptId);
  }

  loadManagersForDept(deptId: number): void {
    this.adminService.getManagers(deptId).subscribe({
      next: (res) => {
        this.availableManagers.set(res?.content || []);
      },
      error: () => {
        this.availableManagers.set([]);
      }
    });
  }

  submitCreateUser(): void {
    const firstName = this.userFirstName().trim();
    const email = this.userEmail().trim();
    if (!firstName) {
      this.state.showNotification('error', 'First name is required.');
      return;
    }
    if (!email) {
      this.state.showNotification('error', 'Email address is required.');
      return;
    }

    const role = this.userRole();
    const deptId = this.userDepartmentId();
    const mgrId = this.userManagerId();

    if ((role === 'EMPLOYEE' || role === 'AGENT') && !deptId) {
      this.state.showNotification('error', `Department is required for ${role} role.`);
      return;
    }
    if ((role === 'EMPLOYEE' || role === 'AGENT') && !mgrId) {
      this.state.showNotification('error', `Reporting Manager is required for ${role} role.`);
      return;
    }

    const payload: CreateUserRequest = {
      firstName,
      lastName: this.userLastName().trim() || undefined,
      email,
      phone: this.userPhone().trim() || undefined,
      designation: this.userDesignation().trim() || undefined,
      role,
      departmentId: (role === 'ADMIN' ? null : deptId),
      managerId: (role === 'MANAGER' || role === 'ADMIN' ? null : mgrId),
      dateOfJoining: this.userDateOfJoining() || undefined,
      timezone: this.userTimezone() || 'Asia/Kolkata'
    };

    this.isSubmittingUser.set(true);
    this.adminService.createUser(payload).subscribe({
      next: (created) => {
        this.isSubmittingUser.set(false);
        this.state.showNotification('success', `User ${created.firstName} (${created.role}) created successfully!`);
        this.closeCreateUserModal();
        this.loadUsersPage(0);
        this.loadAgentsList();
      },
      error: (err) => {
        this.isSubmittingUser.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to create user.');
      }
    });
  }

  // ===================== CONFIGURATION TABS =====================
  switchConfigTab(tab: 'departments' | 'categories' | 'subcategories' | 'holidays' | 'sla-policies' | 'skills'): void {
    this.activeConfigTab.set(tab);
    this.loadConfigTab(tab);
  }

  loadConfigTab(tab: string): void {
    if (tab === 'departments') {
      this.loadDepartmentsConfig();
    } else if (tab === 'categories') {
      this.loadCategoriesConfig(this.configCategoryDeptFilter() ?? undefined);
    } else if (tab === 'subcategories') {
      this.loadSubCategoriesConfig(this.configSubCatCategoryFilter() ?? undefined);
    } else if (tab === 'holidays') {
      this.loadHolidaysConfig();
    } else if (tab === 'sla-policies') {
      this.loadSlaPoliciesConfig();
    } else if (tab === 'skills') {
      this.loadSkills();
    }
  }

  // 1. Departments Config
  loadDepartmentsConfig(): void {
    this.adminService.getDepartments().subscribe({
      next: (depts) => {
        this.configDepartmentsList.set(depts || []);
        this.departmentsList.set(depts || []);
      },
      error: () => {}
    });
  }

  openAddDeptModal(): void {
    this.newDeptCode.set('');
    this.newDeptName.set('');
    this.newDeptDesc.set('');
    this.newDeptTimezone.set('Asia/Kolkata');
    this.showAddDeptModal.set(true);
  }

  closeAddDeptModal(): void {
    this.showAddDeptModal.set(false);
  }

  submitAddDept(): void {
    const code = this.newDeptCode().trim().toUpperCase();
    const name = this.newDeptName().trim();
    if (!code || !name) {
      this.state.showNotification('error', 'Department code and name are required.');
      return;
    }

    const payload: CreateDepartmentRequest = {
      code,
      name,
      description: this.newDeptDesc().trim() || undefined,
      timezone: this.newDeptTimezone() || 'Asia/Kolkata'
    };

    this.isSubmittingDept.set(true);
    this.adminService.createDepartment(payload).subscribe({
      next: (res) => {
        this.isSubmittingDept.set(false);
        this.state.showNotification('success', `Department "${res.name}" created successfully!`);
        this.closeAddDeptModal();
        this.loadDepartmentsConfig();
      },
      error: (err) => {
        this.isSubmittingDept.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to create department.');
      }
    });
  }

  // 2. Categories Config
  loadCategoriesConfig(deptId?: number): void {
    this.adminService.getCategories(deptId).subscribe({
      next: (cats) => {
        this.configCategoriesList.set(cats || []);
        if (cats && cats.length > 0 && !this.newSubCatCatId()) {
          this.newSubCatCatId.set(cats[0].id);
        }
      },
      error: () => {}
    });
  }

  onConfigCategoryDeptFilterChange(deptId: number | null): void {
    this.configCategoryDeptFilter.set(deptId);
    this.loadCategoriesConfig(deptId ?? undefined);
  }

  openAddCatModal(): void {
    this.newCatName.set('');
    this.newCatDesc.set('');
    if (!this.newCatDeptId() && this.departmentsList().length) {
      this.newCatDeptId.set(this.departmentsList()[0].id);
    }
    this.showAddCatModal.set(true);
  }

  closeAddCatModal(): void {
    this.showAddCatModal.set(false);
  }

  submitAddCat(): void {
    const name = this.newCatName().trim();
    const deptId = this.newCatDeptId();
    if (!name || !deptId) {
      this.state.showNotification('error', 'Category name and department are required.');
      return;
    }

    const payload: CreateCategoryRequest = {
      departmentId: deptId,
      name,
      description: this.newCatDesc().trim() || undefined
    };

    this.isSubmittingCat.set(true);
    this.adminService.createCategory(payload).subscribe({
      next: (res) => {
        this.isSubmittingCat.set(false);
        this.state.showNotification('success', `Category "${res.name}" created successfully!`);
        this.closeAddCatModal();
        this.loadCategoriesConfig(this.configCategoryDeptFilter() ?? undefined);
      },
      error: (err) => {
        this.isSubmittingCat.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to create category.');
      }
    });
  }

  // 3. SubCategories Config
  loadSubCategoriesConfig(categoryId?: number): void {
    this.adminService.getSubCategories(categoryId).subscribe({
      next: (subs) => {
        this.configSubCategoriesList.set(subs || []);
      },
      error: () => {}
    });
  }

  onConfigSubCatCategoryFilterChange(catId: number | null): void {
    this.configSubCatCategoryFilter.set(catId);
    this.loadSubCategoriesConfig(catId ?? undefined);
  }

  openAddSubCatModal(): void {
    this.newSubCatName.set('');
    this.newSubCatDesc.set('');
    this.newSubCatPriority.set('MEDIUM');
    if (!this.newSubCatCatId() && this.configCategoriesList().length) {
      this.newSubCatCatId.set(this.configCategoriesList()[0].id);
    }
    this.selectedSubCatSkillIds.set([]);
    this.loadSkills();
    this.showAddSubCatModal.set(true);
  }

  closeAddSubCatModal(): void {
    this.showAddSubCatModal.set(false);
  }

  toggleSubCatSkill(skillId: number): void {
    const current = this.selectedSubCatSkillIds();
    if (current.includes(skillId)) {
      this.selectedSubCatSkillIds.set(current.filter((id) => id !== skillId));
    } else {
      this.selectedSubCatSkillIds.set([...current, skillId]);
    }
  }

  isSubCatSkillSelected(skillId: number): boolean {
    return this.selectedSubCatSkillIds().includes(skillId);
  }

  submitAddSubCat(): void {
    const name = this.newSubCatName().trim();
    const catId = this.newSubCatCatId();
    if (!name || !catId) {
      this.state.showNotification('error', 'Subcategory name and parent category are required.');
      return;
    }

    const payload: CreateSubCategoryRequest = {
      categoryId: catId,
      name,
      description: this.newSubCatDesc().trim() || undefined,
      priority: this.newSubCatPriority(),
      skillIds: this.selectedSubCatSkillIds().length > 0 ? this.selectedSubCatSkillIds() : undefined
    };

    this.isSubmittingSubCat.set(true);
    this.adminService.createSubCategory(payload).subscribe({
      next: (res) => {
        this.isSubmittingSubCat.set(false);
        this.state.showNotification('success', `Subcategory "${res.name}" created successfully!`);
        this.closeAddSubCatModal();
        this.loadSubCategoriesConfig(this.configSubCatCategoryFilter() ?? undefined);
      },
      error: (err) => {
        this.isSubmittingSubCat.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to create subcategory.');
      }
    });
  }

  // 4. Holiday Calendar Config
  loadHolidaysConfig(): void {
    const deptId = this.holidayDeptFilter() ?? undefined;
    const year = this.holidayYearFilter();
    this.adminService.getHolidays(deptId, year).subscribe({
      next: (holidays) => {
        this.holidaysList.set(holidays || []);
      },
      error: () => {
        this.holidaysList.set([]);
      }
    });
  }

  onHolidayDeptFilterChange(deptId: number | null): void {
    this.holidayDeptFilter.set(deptId);
    this.loadHolidaysConfig();
  }

  onHolidayYearFilterChange(year: number): void {
    this.holidayYearFilter.set(year);
    this.loadHolidaysConfig();
  }

  openAddHolidayModal(): void {
    this.newHolidayName.set('');
    this.newHolidayDate.set('');
    if (!this.newHolidayDeptId() && this.departmentsList().length) {
      this.newHolidayDeptId.set(this.departmentsList()[0].id);
    }
    this.showAddHolidayModal.set(true);
  }

  closeAddHolidayModal(): void {
    this.showAddHolidayModal.set(false);
  }

  submitAddHoliday(): void {
    const name = this.newHolidayName().trim();
    const date = this.newHolidayDate().trim();
    const deptId = this.newHolidayDeptId();
    if (!name || !date || !deptId) {
      this.state.showNotification('error', 'Holiday name, date, and department are required.');
      return;
    }

    const payload: CreateHolidayRequest = {
      departmentId: deptId,
      holidayDate: date,
      holidayName: name
    };

    this.isSubmittingHoliday.set(true);
    this.adminService.createHoliday(payload).subscribe({
      next: (res) => {
        this.isSubmittingHoliday.set(false);
        this.state.showNotification('success', `Holiday "${res.holidayName}" added to calendar!`);
        this.closeAddHolidayModal();
        this.loadHolidaysConfig();
      },
      error: (err) => {
        this.isSubmittingHoliday.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to add holiday.');
      }
    });
  }

  deleteHoliday(id: number): void {
    if (!confirm('Are you sure you want to remove this holiday from the calendar?')) return;
    this.isDeletingHoliday.update((m) => ({ ...m, [id]: true }));
    this.adminService.deleteHoliday(id).subscribe({
      next: () => {
        this.isDeletingHoliday.update((m) => ({ ...m, [id]: false }));
        this.state.showNotification('success', 'Holiday removed from calendar successfully.');
        this.loadHolidaysConfig();
      },
      error: (err) => {
        this.isDeletingHoliday.update((m) => ({ ...m, [id]: false }));
        this.state.showNotification('error', err?.error?.message || 'Failed to delete holiday.');
      }
    });
  }

  // 5. Skills Directory Config
  loadSkills(): void {
    this.isLoadingSkills.set(true);
    this.adminService.getSkills().subscribe({
      next: (skills) => {
        this.isLoadingSkills.set(false);
        this.skillsList.set(skills || []);
      },
      error: () => {
        this.isLoadingSkills.set(false);
        this.skillsList.set([]);
      }
    });
  }

  openAddSkillModal(): void {
    this.newSkillName.set('');
    this.newSkillDesc.set('');
    this.showAddSkillModal.set(true);
  }

  closeAddSkillModal(): void {
    this.showAddSkillModal.set(false);
  }

  submitAddSkill(): void {
    const name = this.newSkillName().trim();
    if (!name) {
      this.state.showNotification('error', 'Skill name is required.');
      return;
    }

    const payload: CreateSkillRequest = {
      name,
      description: this.newSkillDesc().trim() || undefined
    };

    this.isSubmittingSkill.set(true);
    this.adminService.createSkill(payload).subscribe({
      next: (res) => {
        this.isSubmittingSkill.set(false);
        this.state.showNotification('success', `Skill "${res.name}" registered successfully!`);
        this.closeAddSkillModal();
        this.loadSkills();
      },
      error: (err) => {
        this.isSubmittingSkill.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to create skill.');
      }
    });
  }

  // 6. SLA Policies Config
  loadSlaPoliciesConfig(): void {
    this.isLoadingSla.set(true);
    this.adminService.getSlaPolicies(this.slaPolicyDeptFilter() ?? undefined).subscribe({
      next: (policies) => {
        this.isLoadingSla.set(false);
        this.slaPoliciesList.set(policies || []);
      },
      error: () => {
        this.isLoadingSla.set(false);
        this.slaPoliciesList.set([]);
      }
    });
  }

  onSlaPolicyDeptFilterChange(deptId: number | null): void {
    this.slaPolicyDeptFilter.set(deptId);
    this.loadSlaPoliciesConfig();
  }

  openAddSlaModal(): void {
    if (!this.newSlaDeptId() && this.departmentsList().length) {
      this.newSlaDeptId.set(this.departmentsList()[0].id);
    }
    if (this.configSubCategoriesList().length === 0) {
      this.loadSubCategoriesConfig();
    }
    this.newSlaSubCatId.set(null);
    this.newSlaDuration.set(8);
    this.newSlaWarning.set(4);
    this.showAddSlaModal.set(true);
  }

  closeAddSlaModal(): void {
    this.showAddSlaModal.set(false);
  }

  submitAddSla(): void {
    const deptId = this.newSlaDeptId();
    if (!deptId) {
      this.state.showNotification('error', 'Department is required for SLA policy.');
      return;
    }

    const subCatId = this.newSlaSubCatId();
    if (!subCatId) {
      this.state.showNotification('error', 'Subcategory is required for SLA policy.');
      return;
    }

    const duration = this.newSlaDuration();
    const warning = this.newSlaWarning();
    if (!duration || duration <= 0) {
      this.state.showNotification('error', 'Resolution time must be greater than 0 hours.');
      return;
    }

    const payload: CreateSlaPolicyRequest = {
      departmentId: deptId,
      subCategoryId: subCatId,
      durationMinutes: Math.round(duration * 60),
      warningMinutes: warning && warning > 0 ? Math.round(warning * 60) : undefined
    };

    this.isSubmittingSla.set(true);
    this.adminService.createSlaPolicy(payload).subscribe({
      next: () => {
        this.isSubmittingSla.set(false);
        this.state.showNotification('success', 'SLA Policy created successfully!');
        this.closeAddSlaModal();
        this.loadSlaPoliciesConfig();
      },
      error: (err) => {
        this.isSubmittingSla.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to create SLA policy.');
      }
    });
  }

  // 7. Agent Skills Assignment Modal
  openAgentSkillsModal(agent: EmployeeResponse): void {
    this.selectedAgentForSkills.set(agent);
    this.selectedAgentSkillIds.set([]);
    this.showAgentSkillsModal.set(true);
    this.isLoadingAgentSkills.set(true);

    if (this.skillsList().length === 0) {
      this.loadSkills();
    }

    this.adminService.getAgentSkills(agent.id).subscribe({
      next: (skills) => {
        this.selectedAgentSkillIds.set(skills ? skills.map((s) => s.id) : []);
        this.isLoadingAgentSkills.set(false);
      },
      error: () => {
        this.selectedAgentSkillIds.set([]);
        this.isLoadingAgentSkills.set(false);
      }
    });
  }

  closeAgentSkillsModal(): void {
    this.showAgentSkillsModal.set(false);
    this.selectedAgentForSkills.set(null);
  }

  toggleAgentSkill(skillId: number): void {
    const current = this.selectedAgentSkillIds();
    if (current.includes(skillId)) {
      this.selectedAgentSkillIds.set(current.filter((id) => id !== skillId));
    } else {
      this.selectedAgentSkillIds.set([...current, skillId]);
    }
  }

  isAgentSkillSelected(skillId: number): boolean {
    return this.selectedAgentSkillIds().includes(skillId);
  }

  submitAgentSkills(): void {
    const agent = this.selectedAgentForSkills();
    if (!agent) return;

    this.isSubmittingAgentSkills.set(true);
    this.adminService.assignAgentSkills(agent.id, { skillIds: this.selectedAgentSkillIds() }).subscribe({
      next: () => {
        this.isSubmittingAgentSkills.set(false);
        this.state.showNotification('success', `Skills updated successfully for agent ${agent.firstName}!`);
        this.closeAgentSkillsModal();
      },
      error: (err) => {
        this.isSubmittingAgentSkills.set(false);
        this.state.showNotification('error', err?.error?.message || 'Failed to update agent skills.');
      }
    });
  }

  getDepartmentName(deptId?: number | null): string {
    if (!deptId) return 'All / General';
    const dept = this.departmentsList().find((d) => d.id === deptId);
    return dept ? dept.name : `Dept #${deptId}`;
  }

  formatHolidayDay(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
  }

  getCategoryName(catId?: number | null): string {
    if (!catId) return 'N/A';
    const cat = this.configCategoriesList().find((c) => c.id === catId);
    return cat ? cat.name : `Cat #${catId}`;
  }
}
