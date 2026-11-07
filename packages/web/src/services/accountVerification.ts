import { apiClient } from "@ebike/shared-code/api";

export type Challenge = { challengeId: string; expiresInSeconds: number; retryAfterSeconds: number; message: string };
export type VerificationChannel = "email" | "phone";
export const accountVerificationAPI = {
  availability: async () => (await apiClient.get<{emailAvailable: boolean; phoneAvailable: boolean}>("/auth/verification/availability")).data,
  request: async (channel: VerificationChannel) => (await apiClient.post<Challenge>(`/auth/verification/${channel}/request`)).data,
  confirm: async (channel: VerificationChannel, challengeId: string, code: string) => apiClient.post(`/auth/verification/${channel}/confirm`, { challengeId, code }),
  requestReset: async (email: string) => (await apiClient.post<Challenge>("/auth/password-reset/request", { email })).data,
  reset: async (challengeId: string, code: string, newPassword: string) => apiClient.post("/auth/password-reset/confirm", { challengeId, code, newPassword }),
};
export const accountError = (error: unknown) => {
  const e = error as { response?: { data?: { detail?: string; message?: string } }; message?: string };
  return e?.response?.data?.detail || e?.response?.data?.message || e?.message || "Không thể xử lý. Vui lòng thử lại.";
};
