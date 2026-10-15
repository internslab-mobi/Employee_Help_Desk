export type NotificationType =
  | 'TICKET_CREATED'
  | 'TICKET_ASSIGNED'
  | 'TICKET_REASSIGNED'
  | 'TICKET_STATUS_CHANGED'
  | 'TICKET_RESOLVED'
  | 'TICKET_REOPENED'
  | 'TICKET_WITHDRAWN'
  | 'TICKET_MESSAGE'
  | 'SLA_WARNING'
  | 'SLA_BREACHED'
  | 'SYSTEM_ALERT';

export interface NotificationItem {
  id: number;
  recipientId: number;
  ticketId?: number;
  ticketNumber?: string;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface UnreadNotificationCountResponse {
  unreadCount: number;
}

export interface NotificationUpdateResponse {
  id: number;
  recipientId?: number;
  ticketId?: number;
  ticketNumber?: string;
  type?: NotificationType;
  title?: string;
  message?: string;
  read: boolean;
  updatedAt?: string;
}

export interface NotificationPageResponse {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
}
