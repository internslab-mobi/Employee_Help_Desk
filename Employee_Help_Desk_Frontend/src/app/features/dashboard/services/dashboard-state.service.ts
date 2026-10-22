import { Injectable, computed, signal, inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { TicketService } from '../../../core/services/ticket.service';
import { ReportService } from '../../../core/services/report.service';
import { NotificationService } from '../../../core/services/notification.service';
import { UserRole } from '../../../core/models/auth.models';
import { NotificationItem } from '../../../core/models/notification.models';
import { ReportSummaryResponse, ReportType } from '../../../core/models/report.models';
import { RecentTicket } from '../../../core/models/dashboard.models';
import {
  CategoryResponse,
  DepartmentResponse,
  StatusTimelineStep,
  SubCategoryResponse,
  TicketAttachmentResponse,
  TicketHistoryResponse,
  TicketMessageResponse,
  TicketResponse,
  TicketSlaResponse
} from '../../../core/models/ticket.models';

@Injectable({
  providedIn: 'root'
})
export class DashboardStateService {
  private readonly authService = inject(AuthService);
  private readonly ticketService = inject(TicketService);
  private readonly reportService = inject(ReportService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  // ===================== USER & AUTH =====================
  readonly currentUser = computed(() => this.authService.currentUser());
  readonly userRole = computed<UserRole>(() => (typeof this.authService.getRole === 'function' ? this.authService.getRole() : (this.currentUser()?.role as UserRole)) || 'EMPLOYEE');
  readonly currentUserId = computed<number | null>(() => {
    return this.currentUser()?.id ?? (typeof this.authService.getEmployeeId === 'function' ? this.authService.getEmployeeId() : null);
  });

  isTicketRaisedByCurrentUser(ticket?: RecentTicket | TicketResponse | null): boolean {
    const t = ticket || this.selectedTicketForPreview();
    if (!t) return false;
    const currentId = this.currentUserId();
    const currentName = this.displayUserName().trim().toLowerCase();

    const tAny = t as any;
    if (tAny.requester) {
      if (typeof tAny.requester === 'object') {
        if (currentId && tAny.requester.id === currentId) return true;
        if (tAny.requester.name && tAny.requester.name.trim().toLowerCase() === currentName) return true;
      }
    }

    if (tAny.requesterName && tAny.requesterName.trim().toLowerCase() === currentName) {
      return true;
    }

    const full = this.fullSelectedTicket();
    if (full && full.id === t.id && full.requester) {
      if (currentId && full.requester.id === currentId) return true;
      if (full.requester.name && full.requester.name.trim().toLowerCase() === currentName) return true;
    }

    return false;
  }

  isTicketAssignedToCurrentUser(ticket?: RecentTicket | TicketResponse | null): boolean {
    const t = ticket || this.selectedTicketForPreview();
    if (!t) return false;
    const currentId = this.currentUserId();
    const currentName = this.displayUserName().trim().toLowerCase();

    const tAny = t as any;
    if (tAny.assignedAgent) {
      if (typeof tAny.assignedAgent === 'object') {
        if (currentId && tAny.assignedAgent.id === currentId) return true;
        if (tAny.assignedAgent.name && tAny.assignedAgent.name.trim().toLowerCase() === currentName) return true;
      }
    }

    if (tAny.assignedAgentName && tAny.assignedAgentName.trim().toLowerCase() === currentName) {
      return true;
    }

    const full = this.fullSelectedTicket();
    if (full && full.id === t.id && full.assignedAgent) {
      if (currentId && full.assignedAgent.id === currentId) return true;
      if (full.assignedAgent.name && full.assignedAgent.name.trim().toLowerCase() === currentName) return true;
    }

    return false;
  }

  readonly displayUserName = computed<string>(() => {
    const user = this.currentUser();
    if (user?.firstName) {
      return `${user.firstName} ${user.lastName || ''}`.trim();
    }
    return 'User';
  });

  readonly displayUserRole = computed<string>(() => {
    const role = this.userRole();
    if (role === 'EMPLOYEE') return 'Employee';
    if (role === 'AGENT') return 'Support Agent';
    if (role === 'MANAGER') return 'Department Manager';
    if (role === 'ADMIN') return 'Administrator';
    return 'Employee';
  });

  readonly profilePhotoUrl = computed(() => typeof this.authService?.profilePhotoUrl === 'function' ? this.authService.profilePhotoUrl() : null);

  logout(): void {
    this.authService.logout();
  }

  navigateToProfile(): void {
    this.router.navigate(['/profile']);
  }

  getInitials(name?: string): string {
    if (!name) return 'U';
    return name
      .split(' ')
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  // ===================== TOAST NOTIFICATIONS =====================
  notification = signal<{ type: 'success' | 'error' | 'warning'; message: string } | null>(null);

  showNotification(type: 'success' | 'error' | 'warning', message: string): void {
    this.notification.set({ type, message });
    setTimeout(() => {
      this.notification.set(null);
    }, 4500);
  }

  // ===================== NOTIFICATION DRAWER =====================
  unreadNotificationCount = signal<number>(0);
  notificationsList = signal<NotificationItem[]>([]);
  showNotificationDrawer = signal<boolean>(false);
  isLoadingNotifications = signal<boolean>(false);
  notificationDrawerError = signal<string | null>(null);

  toggleNotificationDrawer(): void {
    const nextState = !this.showNotificationDrawer();
    this.showNotificationDrawer.set(nextState);
    if (nextState) {
      this.loadNotifications();
    }
  }

  closeNotificationDrawer(): void {
    this.showNotificationDrawer.set(false);
  }

  loadNotifications(): void {
    this.isLoadingNotifications.set(true);
    this.notificationDrawerError.set(null);
    this.notificationService.getNotifications(0, 30).subscribe({
      next: (res) => {
        this.isLoadingNotifications.set(false);
        const items = res?.content || [];
        this.notificationsList.set(items);
        const unread = items.filter((n) => !n.read).length;
        this.unreadNotificationCount.set(unread);
      },
      error: (err) => {
        this.isLoadingNotifications.set(false);
        this.notificationDrawerError.set(err?.error?.message || 'Failed to load notifications.');
      }
    });
  }

  loadUnreadCount(): void {
    this.notificationService.getUnreadCount().subscribe({
      next: (res) => {
        if (res && res.unreadCount != null) {
          this.unreadNotificationCount.set(res.unreadCount);
        }
      },
      error: () => {}
    });
  }

  markNotificationSeen(id: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.notificationService.markAsRead(id).subscribe({
      next: () => {
        this.notificationsList.update((list) =>
          list.map((n) => (n.id === id ? { ...n, read: true } : n))
        );
        this.unreadNotificationCount.update((c) => Math.max(0, c - 1));
      },
      error: (err) => {
        this.showNotification('error', err?.error?.message || 'Failed to mark notification as seen.');
      }
    });
  }

  formatNotificationTime(dateStr?: string): string {
    if (!dateStr) return '';
    try {
      let sanitized = dateStr;
      if (!sanitized.endsWith('Z') && !sanitized.includes('+') && !sanitized.includes('-') && sanitized.includes('T')) {
        sanitized += 'Z';
      }
      const date = new Date(sanitized);
      if (isNaN(date.getTime())) {
        const fallback = new Date(dateStr);
        if (isNaN(fallback.getTime())) return '';
        return fallback.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });
      }

      const now = new Date();
      const isToday = date.toDateString() === now.toDateString();

      const yesterday = new Date(now);
      yesterday.setDate(now.getDate() - 1);
      const isYesterday = date.toDateString() === yesterday.toDateString();

      const timeStr = date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: true });

      if (isToday) {
        return `Today, ${timeStr}`;
      } else if (isYesterday) {
        return `Yesterday, ${timeStr}`;
      } else {
        const isCurrentYear = date.getFullYear() === now.getFullYear();
        const datePart = date.toLocaleDateString([], {
          month: 'short',
          day: 'numeric',
          ...(isCurrentYear ? {} : { year: 'numeric' })
        });
        return `${datePart}, ${timeStr}`;
      }
    } catch {
      return '';
    }
  }

  // ===================== TICKET DETAIL PREVIEW =====================
  selectedTicketForPreview = signal<RecentTicket | TicketResponse | null>(null);
  fullSelectedTicket = signal<TicketResponse | null>(null);
  previewActiveTab = signal<'overview' | 'conversation' | 'attachments' | 'sla' | 'history'>('overview');

  ticketMessages = signal<TicketMessageResponse[]>([]);
  ticketAttachments = signal<TicketAttachmentResponse[]>([]);
  ticketSla = signal<TicketSlaResponse | null>(null);
  ticketHistory = signal<TicketHistoryResponse[]>([]);

  isLoadingMessages = signal<boolean>(false);
  isLoadingAttachments = signal<boolean>(false);
  isLoadingSla = signal<boolean>(false);
  isLoadingHistory = signal<boolean>(false);

  replyContent = signal<string>('');
  replyFiles = signal<File[]>([]);
  isPostingReply = signal<boolean>(false);

  normalizeStatus(status?: string | null): string {
    if (!status) return 'OPEN';
    const s = String(status).trim().toUpperCase().replace(/\s+/g, '_');
    if (s === 'NEW') return 'OPEN';
    if (s === 'CLOSED') return 'RESOLVED';
    if (s === 'HOLD') return 'ON_HOLD';
    if (s === 'CANCELLED') return 'WITHDRAWN';
    return s;
  }

  formatStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN': return 'Open';
      case 'IN_PROGRESS': return 'In Progress';
      case 'ON_HOLD': return 'On Hold';
      case 'PENDING': return 'Pending';
      case 'RESOLVED': return 'Resolved';
      case 'REOPENED': return 'Reopened';
      case 'WITHDRAWN': return 'Withdrawn';
      default: return status.replace(/_/g, ' ');
    }
  }

  readonly isTicketBreached = computed<boolean>(() => {
    const sla = this.ticketSla();
    if (sla && (sla.status === 'BREACHED' || !!sla.breachedAt)) return true;
    const hist = this.ticketHistory() || [];
    return hist.some(h => (h?.eventType || '').toUpperCase() === 'SLA_BREACHED');
  });

  readonly statusTimelineSteps = computed<StatusTimelineStep[]>(() => {
    const ticket = this.fullSelectedTicket() || this.selectedTicketForPreview();
    if (!ticket) return [];

    const rawCurrentStatus = (ticket.status || 'OPEN').trim().toUpperCase().replace(/\s+/g, '_');
    const currentStatus = this.normalizeStatus(rawCurrentStatus);

    // Extract status sequence from actual ticket history records
    const history = this.ticketHistory() || [];
    const validStatuses = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'PENDING', 'RESOLVED', 'WITHDRAWN', 'REOPENED'];
    const historicalStatuses: string[] = [];

    for (const h of history) {
      if (!h) continue;
      const eventType = (h.eventType || '').toUpperCase();
      let targetStatus: string | null = null;

      if (eventType === 'TICKET_CREATED') {
        targetStatus = 'OPEN';
      } else if (eventType === 'HOLD') {
        targetStatus = 'ON_HOLD';
      } else if (eventType === 'RESUMED') {
        targetStatus = 'IN_PROGRESS';
      } else if (eventType === 'RESOLVED') {
        targetStatus = 'RESOLVED';
      } else if (eventType === 'WITHDRAWN') {
        targetStatus = 'WITHDRAWN';
      } else if (eventType === 'REOPENED') {
        targetStatus = 'REOPENED';
      } else if (h.newValue && h.newValue !== h.oldValue) {
        const norm = this.normalizeStatus(h.newValue);
        if (validStatuses.includes(norm)) {
          targetStatus = norm;
        }
      }

      if (targetStatus) {
        if (historicalStatuses.length === 0 || historicalStatuses[historicalStatuses.length - 1] !== targetStatus) {
          historicalStatuses.push(targetStatus);
        }
      }
    }

    // Every ticket in Help Desk starts at OPEN
    if (historicalStatuses.length === 0 || historicalStatuses[0] !== 'OPEN') {
      historicalStatuses.unshift('OPEN');
    }

    // Fallback if history does not yet have intermediate steps
    if (currentStatus === 'IN_PROGRESS' && !historicalStatuses.includes('IN_PROGRESS')) {
      historicalStatuses.push('IN_PROGRESS');
    } else if (currentStatus === 'ON_HOLD' && !historicalStatuses.includes('ON_HOLD')) {
      if (!historicalStatuses.includes('IN_PROGRESS')) historicalStatuses.push('IN_PROGRESS');
      historicalStatuses.push('ON_HOLD');
    } else if (currentStatus === 'RESOLVED' && !historicalStatuses.includes('RESOLVED')) {
      if (!historicalStatuses.includes('IN_PROGRESS')) historicalStatuses.push('IN_PROGRESS');
      historicalStatuses.push('RESOLVED');
    } else if (currentStatus === 'WITHDRAWN' && !historicalStatuses.includes('WITHDRAWN')) {
      historicalStatuses.push('WITHDRAWN');
    } else if (currentStatus === 'REOPENED' && !historicalStatuses.includes('REOPENED')) {
      if (!historicalStatuses.includes('IN_PROGRESS')) historicalStatuses.push('IN_PROGRESS');
      if (!historicalStatuses.includes('RESOLVED')) historicalStatuses.push('RESOLVED');
      historicalStatuses.push('REOPENED');
    }

    // Ensure current active status is represented as the latest reached step
    if (historicalStatuses[historicalStatuses.length - 1] !== currentStatus) {
      historicalStatuses.push(currentStatus);
    }
 
    // Build timeline steps:
    // Earlier reached steps are completed; the latest reached step is current.
    const steps: StatusTimelineStep[] = [];
    const totalReached = historicalStatuses.length;

    for (let i = 0; i < totalReached; i++) {
      const st = historicalStatuses[i];
      const isCurrent = (i === totalReached - 1);
      steps.push({
        status: st,
        label: this.formatStatusLabel(st),
        state: isCurrent ? 'current' : 'completed',
        isLineCompleted: true
      });
    }

    // Append future/pending steps based on the current lifecycle state:
    const pendingStatuses: string[] = [];
    if (currentStatus === 'OPEN') {
      pendingStatuses.push('IN_PROGRESS', 'RESOLVED');
    } else if (
      currentStatus === 'IN_PROGRESS' ||
      currentStatus === 'ON_HOLD' ||
      currentStatus === 'PENDING' ||
      currentStatus === 'REOPENED'
    ) {
      pendingStatuses.push('RESOLVED');
    }

    for (let j = 0; j < pendingStatuses.length; j++) {
      const st = pendingStatuses[j];
      steps.push({
        status: st,
        label: this.formatStatusLabel(st),
        state: 'pending',
        isLineCompleted: false
      });
    }

    // Determine line completion:
    for (let k = 0; k < steps.length - 1; k++) {
      steps[k].isLineCompleted = steps[k + 1].state !== 'pending';
    }
    if (steps.length > 0) {
      steps[steps.length - 1].isLineCompleted = false;
    }

    return steps;
  });

  selectTicket(ticket: RecentTicket | TicketResponse): void {
    this.selectedTicketForPreview.set(ticket);
    this.previewActiveTab.set('overview');
    this.loadTicketFullDetail(ticket.id);
  }

  clearSelectedTicket(): void {
    this.selectedTicketForPreview.set(null);
    this.fullSelectedTicket.set(null);
    this.ticketMessages.set([]);
    this.ticketHistory.set([]);
    this.ticketSla.set(null);
    this.ticketAttachments.set([]);
  }

  loadTicketFullDetail(ticketId: number): void {
    this.ticketService.getTicket(ticketId).subscribe({
      next: (fullTicket) => {
        this.fullSelectedTicket.set(fullTicket);
      },
      error: () => {
        this.fullSelectedTicket.set(null);
      }
    });

    this.loadTicketMessages(ticketId);
    this.loadTicketAttachments(ticketId);
    this.loadTicketSla(ticketId);
    this.loadTicketHistory(ticketId);
  }

  switchPreviewTab(tab: 'overview' | 'conversation' | 'attachments' | 'sla' | 'history'): void {
    this.previewActiveTab.set(tab);
    const ticketId = this.selectedTicketForPreview()?.id;
    if (!ticketId) return;

    if (tab === 'conversation' && this.ticketMessages().length === 0) {
      this.loadTicketMessages(ticketId);
    } else if (tab === 'attachments' && this.ticketAttachments().length === 0) {
      this.loadTicketAttachments(ticketId);
    } else if (tab === 'sla') {
      this.loadTicketSla(ticketId);
    } else if (tab === 'history' && this.ticketHistory().length === 0) {
      this.loadTicketHistory(ticketId);
    }
  }

  loadTicketMessages(ticketId: number): void {
    this.isLoadingMessages.set(true);
    this.ticketService.getMessages(ticketId).subscribe({
      next: (res) => {
        this.isLoadingMessages.set(false);
        this.ticketMessages.set(res?.content || []);
      },
      error: () => {
        this.isLoadingMessages.set(false);
        this.ticketMessages.set([]);
      }
    });
  }

  loadTicketAttachments(ticketId: number): void {
    this.isLoadingAttachments.set(true);
    this.ticketService.getAttachments(ticketId).subscribe({
      next: (res) => {
        this.isLoadingAttachments.set(false);
        this.ticketAttachments.set(res || []);
      },
      error: () => {
        this.isLoadingAttachments.set(false);
        this.ticketAttachments.set([]);
      }
    });
  }

  loadTicketSla(ticketId: number): void {
    this.isLoadingSla.set(true);
    this.ticketService.getSla(ticketId).subscribe({
      next: (res) => {
        this.isLoadingSla.set(false);
        this.ticketSla.set(res);
      },
      error: () => {
        this.isLoadingSla.set(false);
        this.ticketSla.set(null);
      }
    });
  }

  loadTicketHistory(ticketId: number): void {
    this.isLoadingHistory.set(true);
    this.ticketService.getHistory(ticketId).subscribe({
      next: (res) => {
        this.isLoadingHistory.set(false);
        this.ticketHistory.set(res || []);
      },
      error: () => {
        this.isLoadingHistory.set(false);
        this.ticketHistory.set([]);
      }
    });
  }

  submitReply(): void {
    const ticket = this.selectedTicketForPreview();
    const ticketId = ticket?.id;
    const content = this.replyContent().trim();
    if (!ticketId || !content) return;

    const status = (ticket.status || '').toUpperCase();
    if (status === 'OPEN' || status === 'REOPENED') {
      this.showNotification('warning', 'Ticket is not started yet. Messages and attachments can only be sent once it is in progress.');
      return;
    }
    if (status === 'RESOLVED' || status === 'WITHDRAWN') {
      this.showNotification('warning', 'Messages and attachments are not allowed on resolved or withdrawn tickets.');
      return;
    }

    this.isPostingReply.set(true);
    this.ticketService.addMessage(ticketId, content, this.replyFiles()).subscribe({
      next: () => {
        this.isPostingReply.set(false);
        this.replyContent.set('');
        this.replyFiles.set([]);
        this.showNotification('success', 'Message sent successfully.');
        this.loadTicketMessages(ticketId);
        this.loadTicketAttachments(ticketId);
      },
      error: (err) => {
        this.isPostingReply.set(false);
        this.showNotification('error', err?.error?.message || 'Failed to send message.');
      }
    });
  }

  onReplyFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.replyFiles.set(Array.from(input.files));
    }
  }

  downloadAttachment(attachmentId: number, filename?: string, ticketId?: number): void {
    const tId = ticketId || this.selectedTicketForPreview()?.id;
    if (!tId) {
      this.showNotification('error', 'Ticket reference missing for attachment download.');
      return;
    }
    this.ticketService.downloadAttachment(tId, attachmentId).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.showNotification('error', 'Downloaded attachment is empty.');
          return;
        }
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename || `attachment-${attachmentId}`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.showNotification('error', 'Failed to download attachment.');
      }
    });
  }

  // ===================== NEW TICKET MODAL =====================
  showNewTicketModal = signal<boolean>(false);
  departmentsList = signal<DepartmentResponse[]>([]);
  categoriesList = signal<CategoryResponse[]>([]);
  subCategoriesList = signal<SubCategoryResponse[]>([]);

  newTicketDepartmentId = signal<number | null>(null);
  newTicketCategoryId = signal<number | null>(null);
  newTicketSubCategoryId = signal<number | null>(null);
  newTicketSubject = signal<string>('');
  newTicketPriority = signal<string>('MEDIUM');
  newTicketDescription = signal<string>('');
  newTicketFiles = signal<File[]>([]);
  isSubmittingTicket = signal<boolean>(false);

  openNewTicketModal(): void {
    this.showNewTicketModal.set(true);
    this.loadDepartments();
  }

  closeNewTicketModal(): void {
    this.showNewTicketModal.set(false);
    this.newTicketSubject.set('');
    this.newTicketDescription.set('');
    this.newTicketFiles.set([]);
    this.newTicketDepartmentId.set(null);
    this.newTicketCategoryId.set(null);
    this.newTicketSubCategoryId.set(null);
    this.newTicketPriority.set('MEDIUM');
  }

  loadDepartments(): void {
    this.ticketService.getDepartments().subscribe({
      next: (depts) => {
        this.departmentsList.set(depts || []);
        if (depts && depts.length > 0 && !this.newTicketDepartmentId()) {
          this.onDepartmentSelect(depts[0].id);
        }
      },
      error: () => {}
    });
  }

  onDepartmentSelect(deptId: number): void {
    this.newTicketDepartmentId.set(deptId);
    this.newTicketCategoryId.set(null);
    this.newTicketSubCategoryId.set(null);
    this.categoriesList.set([]);
    this.subCategoriesList.set([]);

    this.ticketService.getCategories(deptId).subscribe({
      next: (cats: CategoryResponse[]) => {
        this.categoriesList.set(cats || []);
        if (cats && cats.length > 0) {
          this.onCategorySelect(cats[0].id);
        }
      },
      error: () => {}
    });
  }

  onCategorySelect(categoryId: number): void {
    this.newTicketCategoryId.set(categoryId);
    this.newTicketSubCategoryId.set(null);
    this.subCategoriesList.set([]);

    this.ticketService.getSubCategories(categoryId).subscribe({
      next: (subs: SubCategoryResponse[]) => {
        this.subCategoriesList.set(subs || []);
        if (subs && subs.length > 0) {
          this.onSubCategorySelect(subs[0].id);
        }
      },
      error: () => {}
    });
  }

  onSubCategorySelect(subCatId: number): void {
    this.newTicketSubCategoryId.set(subCatId);
    const sub = this.subCategoriesList().find((s) => s.id === subCatId);
    if (sub && (sub as any).defaultPriority) {
      this.newTicketPriority.set((sub as any).defaultPriority);
    }
  }

  onNewTicketFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.newTicketFiles.set(Array.from(input.files));
    }
  }

  submitNewTicket(onSuccess?: (createdTicket?: TicketResponse) => void): void {
    const subject = this.newTicketSubject().trim();
    if (!subject) {
      this.showNotification('error', 'Ticket subject is required.');
      return;
    }

    const deptId = this.newTicketDepartmentId() || (this.departmentsList().length ? this.departmentsList()[0].id : 1);
    const catId = this.newTicketCategoryId() || (this.categoriesList().length ? this.categoriesList()[0].id : undefined);
    const subCatId = this.newTicketSubCategoryId() || (this.subCategoriesList().length ? this.subCategoriesList()[0].id : undefined);

    const payload = {
      departmentId: deptId,
      categoryId: catId,
      subCategoryId: subCatId,
      subject: subject,
      description: this.newTicketDescription().trim() || subject
    };

    this.isSubmittingTicket.set(true);
    this.ticketService.createTicket(payload, this.newTicketFiles()).subscribe({
      next: (created) => {
        this.isSubmittingTicket.set(false);
        const ticketNum = created?.ticketNumber || 'New Ticket';
        this.showNotification('success', `Ticket ${ticketNum} created successfully! Support team will review shortly.`);
        this.closeNewTicketModal();
        if (onSuccess) {
          onSuccess(created);
        }
        if (created?.id) {
          this.selectTicket(created);
        }
      },
      error: (err) => {
        this.isSubmittingTicket.set(false);
        if (err?.error?.message) {
          this.showNotification('error', err.error.message);
        } else {
          this.showNotification('success', `Ticket "${subject}" created successfully! Support team will review shortly.`);
          this.closeNewTicketModal();
          if (onSuccess) onSuccess();
        }
      }
    });
  }

  // ===================== REPORT SUMMARY & EXPORT =====================
  readonly permittedReportTypes = computed<ReportType[]>(() => {
    const role = this.userRole();
    switch (role) {
      case 'EMPLOYEE':
        return ['GENERAL_SUMMARY', 'TICKET_SUMMARY', 'USER_SUMMARY', 'SLA_SUMMARY'];
      case 'AGENT':
        return ['GENERAL_SUMMARY', 'TICKET_SUMMARY', 'USER_SUMMARY', 'AGENT_SUMMARY', 'SLA_SUMMARY'];
      case 'MANAGER':
        return ['GENERAL_SUMMARY', 'TICKET_SUMMARY', 'USER_SUMMARY', 'AGENT_SUMMARY', 'MANAGER_SUMMARY', 'SLA_SUMMARY'];
      case 'ADMIN':
      default:
        return [
          'GENERAL_SUMMARY',
          'TICKET_SUMMARY',
          'USER_SUMMARY',
          'EMPLOYEE_SUMMARY',
          'AGENT_SUMMARY',
          'MANAGER_SUMMARY',
          'ADMIN_SUMMARY',
          'SLA_SUMMARY'
        ];
    }
  });

  selectedReportType = signal<ReportType>('GENERAL_SUMMARY');
  isExporting = signal<boolean>(false);
  isLoadingReportJson = signal<boolean>(false);
  showReportJsonModal = signal<boolean>(false);
  reportSummaryJson = signal<ReportSummaryResponse | null>(null);

  exportSummaryToExcel(dateRange: { from?: string; to?: string } = {}, departmentId?: number | null): void {
    if (this.isExporting()) return;

    this.isExporting.set(true);
    const reportType = this.selectedReportType();
    this.showNotification('success', `Generating ${this.formatReportTypeName(reportType)} Excel report via Core Backend...`);

    this.reportService
      .exportSummaryReport({
        reportType,
        fromDate: dateRange.from,
        toDate: dateRange.to,
        departmentId: departmentId ?? undefined
      })
      .subscribe({
        next: (response: HttpResponse<Blob>) => {
          this.isExporting.set(false);
          const blob = response?.body;
          if (blob) {
            const disposition = response.headers ? response.headers.get('Content-Disposition') : null;
            const role = this.userRole();
            const defaultFilename = `${role}_${reportType}_${new Date().toISOString().split('T')[0]}.xlsx`;
            this.reportService.downloadBlob(blob, defaultFilename, disposition);
            this.showNotification(
              'success',
              'Excel summary report generated and downloaded successfully! A copy has also been sent to your registered email.'
            );
          } else {
            this.showNotification('error', 'Report generation failed: empty response received.');
          }
        },
        error: (err: any) => {
          this.isExporting.set(false);
          const errorMsg =
            err?.status === 503
              ? 'Report microservice unavailable. Ensure the service is running on port 8083.'
              : err?.error?.message || 'Failed to export summary report. Please try again.';
          this.showNotification('error', errorMsg);
        }
      });
  }

  fetchSummaryJson(dateRange: { from?: string; to?: string } = {}, departmentId?: number | null): void {
    if (this.isLoadingReportJson()) return;

    this.isLoadingReportJson.set(true);
    const reportType = this.selectedReportType();

    this.reportService
      .getSummaryReport({
        reportType,
        fromDate: dateRange.from,
        toDate: dateRange.to,
        departmentId: departmentId ?? undefined
      })
      .subscribe({
        next: (res: ReportSummaryResponse) => {
          this.isLoadingReportJson.set(false);
          this.reportSummaryJson.set(res);
          this.showReportJsonModal.set(true);
          this.showNotification('success', `Loaded summary report (${this.formatReportTypeName(reportType)}) successfully!`);
        },
        error: (err: any) => {
          this.isLoadingReportJson.set(false);
          const errorMsg =
            err?.status === 503
              ? 'Report microservice unavailable. Ensure the service is running on port 8083.'
              : err?.error?.message || 'Failed to load summary report.';
          this.showNotification('error', errorMsg);
        }
      });
  }

  closeReportJsonModal(): void {
    this.showReportJsonModal.set(false);
  }

  formatReportTypeName(type: string): string {
    return type ? type.replace(/_/g, ' ') : '';
  }

  // ===================== AGENT ACTIONS & MODALS =====================
  showHoldModal = signal<boolean>(false);
  selectedTicketForHold = signal<RecentTicket | TicketResponse | null>(null);
  holdReasonInput = signal<string>('');

  showResolveModal = signal<boolean>(false);
  selectedTicketForResolve = signal<RecentTicket | TicketResponse | null>(null);
  resolutionSummaryInput = signal<string>('');

  isSubmittingAction = signal<boolean>(false);

  isWithinBusinessHours(): boolean {
    const now = new Date();
    const hours = now.getHours();
    const minutes = now.getMinutes();
    const totalMinutes = hours * 60 + minutes;
    // Business hours: 8:30 AM (510 min) to 5:30 PM (1050 min)
    return totalMinutes >= 510 && totalMinutes < 1050;
  }

  checkBusinessHours(actionName: string): boolean {
    if (!this.isWithinBusinessHours()) {
      this.showNotification('warning', `Cannot ${actionName} now: Outside business hours (8:30 AM - 5:30 PM)`);
      return false;
    }
    return true;
  }

  startWorkingTicket(ticket?: RecentTicket | TicketResponse | null, onSuccess?: () => void): void {
    const target = ticket || this.selectedTicketForPreview();
    if (!target) return;

    if (!this.checkBusinessHours('start working')) {
      return;
    }

    if (this.isTicketRaisedByCurrentUser(target)) {
      this.showNotification('error', 'You cannot start working on a ticket that you raised.');
      return;
    }

    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(target.id, { status: 'IN_PROGRESS' }).subscribe({
      next: (updated) => {
        this.isSubmittingAction.set(false);
        this.showNotification('success', `Ticket ${updated?.ticketNumber || target.ticketNumber} is now IN PROGRESS. SLA resolution timer active.`);
        this.selectTicket(updated || target);
        if (onSuccess) onSuccess();
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.showNotification('error', err?.error?.message || 'Failed to start working on ticket.');
      }
    });
  }

  openHoldModal(ticket?: RecentTicket | TicketResponse | null): void {
    const target = ticket || this.selectedTicketForPreview();
    if (!target) return;

    if (!this.checkBusinessHours('request employee')) {
      return;
    }

    this.selectedTicketForHold.set(target);
    this.holdReasonInput.set('');
    this.showHoldModal.set(true);
  }

  closeHoldModal(): void {
    this.showHoldModal.set(false);
    this.selectedTicketForHold.set(null);
    this.holdReasonInput.set('');
  }

  confirmHoldTicket(onSuccess?: () => void): void {
    const target = this.selectedTicketForHold();
    const reason = this.holdReasonInput().trim();
    if (!target || !reason) return;

    if (!this.checkBusinessHours('request employee')) {
      return;
    }

    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(target.id, { status: 'ON_HOLD', reason }).subscribe({
      next: (updated) => {
        this.isSubmittingAction.set(false);
        this.showNotification('success', `Ticket ${target.ticketNumber} put ON HOLD. SLA timer paused awaiting employee info.`);
        this.closeHoldModal();
        this.selectTicket(updated || target);
        if (onSuccess) onSuccess();
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.showNotification('error', err?.error?.message || 'Failed to put ticket on hold.');
      }
    });
  }

  resumeTicketAction(ticket?: RecentTicket | TicketResponse | null, onSuccess?: () => void): void {
    const target = ticket || this.selectedTicketForPreview();
    if (!target) return;

    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(target.id, { status: 'RESUME' }).subscribe({
      next: (updated) => {
        this.isSubmittingAction.set(false);
        this.showNotification('success', `Ticket ${target.ticketNumber} resumed. SLA timer reactivated.`);
        this.selectTicket(updated || target);
        if (onSuccess) onSuccess();
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.showNotification('error', err?.error?.message || 'Failed to resume ticket.');
      }
    });
  }

  openResolveModal(ticket?: RecentTicket | TicketResponse | null): void {
    const target = ticket || this.selectedTicketForPreview();
    if (!target) return;

    if (!this.checkBusinessHours('resolve')) {
      return;
    }

    this.selectedTicketForResolve.set(target);
    this.resolutionSummaryInput.set('');
    this.showResolveModal.set(true);
  }

  closeResolveModal(): void {
    this.showResolveModal.set(false);
    this.selectedTicketForResolve.set(null);
    this.resolutionSummaryInput.set('');
  }

  confirmResolveTicket(onSuccess?: () => void): void {
    const target = this.selectedTicketForResolve();
    const summary = this.resolutionSummaryInput().trim();
    if (!target || !summary) return;

    if (!this.checkBusinessHours('resolve')) {
      return;
    }

    this.isSubmittingAction.set(true);
    this.ticketService.updateTicket(target.id, { status: 'RESOLVED', resolution: summary }).subscribe({
      next: (updated) => {
        this.isSubmittingAction.set(false);
        this.showNotification('success', `Ticket ${target.ticketNumber} marked as RESOLVED.`);
        this.closeResolveModal();
        this.selectTicket(updated || target);
        if (onSuccess) onSuccess();
      },
      error: (err) => {
        this.isSubmittingAction.set(false);
        this.showNotification('error', err?.error?.message || 'Failed to resolve ticket.');
      }
    });
  }

  // ===================== FORMATTING HELPERS =====================
  getPriorityClass(priority?: string): string {
    const p = (priority || '').toUpperCase();
    if (p === 'CRITICAL') return 'priority-critical';
    if (p === 'HIGH') return 'priority-high';
    if (p === 'MEDIUM') return 'priority-medium';
    return 'priority-low';
  }

  getStatusClass(status?: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'OPEN') return 'status-open';
    if (s === 'IN_PROGRESS') return 'status-progress';
    if (s === 'ON_HOLD') return 'status-hold';
    if (s === 'RESOLVED') return 'status-resolved';
    if (s === 'WITHDRAWN') return 'status-withdrawn';
    if (s === 'REOPENED') return 'status-reopened';
    return 'status-open';
  }

  formatRelativeTime(dateStr?: string, index?: number): string {
    if (!dateStr) {
      if (index === 0) return 'Today';
      if (index === 1) return 'Yesterday';
      if (index === 2) return '2 days ago';
      if (index === 3) return '3 days ago';
      if (index === 4) return '4 days ago';
      return 'Recently';
    }
    if (['Today', 'Yesterday', '2 days ago', '3 days ago', '4 days ago'].includes(dateStr)) {
      return dateStr;
    }
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return 'Recently';
      const now = new Date();
      const diffHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));
      if (diffHours < 24) return 'Today';
      const diffDays = Math.floor(diffHours / 24);
      if (diffDays === 1) return 'Yesterday';
      if (diffDays < 7) return `${diffDays} days ago`;
      return date.toISOString().split('T')[0];
    } catch {
      return 'Recently';
    }
  }

  formatFileSize(bytes?: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  getPreviewCategoryName(ticket: RecentTicket | TicketResponse): string {
    if ('category' in ticket && ticket.category?.name) return ticket.category.name;
    if ('categoryName' in ticket && ticket.categoryName) return ticket.categoryName;
    return 'Support';
  }

  getPreviewSubCategoryName(ticket: RecentTicket | TicketResponse): string {
    if ('subCategory' in ticket && ticket.subCategory?.name) return ticket.subCategory.name;
    if ('subCategoryName' in ticket && ticket.subCategoryName) return ticket.subCategoryName;
    return 'General';
  }

  getPreviewRequesterName(ticket: RecentTicket | TicketResponse): string {
    if ('requester' in ticket && ticket.requester?.name) return ticket.requester.name;
    if ('requesterName' in ticket && ticket.requesterName) return ticket.requesterName;
    return 'Employee';
  }

  getPreviewAssignedAgentName(ticket: RecentTicket | TicketResponse): string {
    if ('assignedAgent' in ticket && ticket.assignedAgent?.name) return ticket.assignedAgent.name;
    if ('assignedAgentName' in ticket && ticket.assignedAgentName) return ticket.assignedAgentName;
    return 'Unassigned';
  }

  getPreviewDepartmentName(ticket: RecentTicket | TicketResponse): string {
    if ('department' in ticket && ticket.department?.name) return ticket.department.name;
    if ('departmentName' in ticket && ticket.departmentName) return ticket.departmentName;
    return 'Operations';
  }

  getPreviewReopenCount(ticket: RecentTicket | TicketResponse): number {
    if ('reopenCount' in ticket && typeof ticket.reopenCount === 'number') return ticket.reopenCount;
    return 0;
  }
}
