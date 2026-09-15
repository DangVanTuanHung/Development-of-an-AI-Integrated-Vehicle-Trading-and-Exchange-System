import { apiClient } from "./client";
import { API_ENDPOINTS } from "./endpoints";
import type { SupportTicket, SupportTicketCreateRequest, SupportTicketPriority, SupportTicketStatus } from "../types";
export const supportAPI = {
  create: async (payload: SupportTicketCreateRequest) => (await apiClient.post<SupportTicket>(API_ENDPOINTS.support.tickets, payload)).data,
  mine: async () => (await apiClient.get<SupportTicket[]>(API_ENDPOINTS.support.mine)).data,
  adminList: async (scope: "admin" | "manager", params?: {status?: string; search?: string}) => (await apiClient.get<SupportTicket[]>(API_ENDPOINTS.support.console(scope), {params})).data,
  update: async (scope: "admin" | "manager", id: number, payload: {status?: SupportTicketStatus; priority?: SupportTicketPriority; staffNote?: string; assignedTo?: string}) => (await apiClient.patch<SupportTicket>(API_ENDPOINTS.support.consoleTicket(scope,id), payload)).data
};
