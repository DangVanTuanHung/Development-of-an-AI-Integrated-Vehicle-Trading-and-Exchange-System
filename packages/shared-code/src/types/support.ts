export type SupportTicketStatus = "OPEN" | "IN_PROGRESS" | "WAITING_CUSTOMER" | "RESOLVED" | "CLOSED";
export type SupportTicketPriority = "LOW" | "NORMAL" | "HIGH" | "URGENT";
export interface SupportTicket {
  id: number; ticketCode: string; requesterName: string; requesterEmail: string; category: string;
  subject: string; message: string; status: SupportTicketStatus; priority: SupportTicketPriority;
  staffNote?: string | null; assignedTo?: string | null; createdAt: string; updatedAt: string; resolvedAt?: string | null;
}
export interface SupportTicketCreateRequest { requesterName: string; requesterEmail: string; category: string; subject: string; message: string; }
