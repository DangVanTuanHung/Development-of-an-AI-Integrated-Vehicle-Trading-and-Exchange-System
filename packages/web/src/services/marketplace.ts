import { apiClient } from "@ebike/shared-code/api";
import { API_BASE_URL } from "@ebike/shared-code/config";

export type MarketplaceCategory = { id: number; name: string; slug: string; parentId: number | null };
export type MarketplaceListing = {
  id: number;
  publicId: string;
  sellerId: number;
  sellerName: string;
  sellerPhone?: string;
  categoryId: number;
  brandId: number | null;
  title: string;
  slug: string;
  description: string;
  listingType: "SALE" | "EXCHANGE" | "SALE_OR_EXCHANGE";
  condition: "NEW" | "LIKE_NEW" | "USED" | "RESTORED" | "DAMAGED";
  status: string;
  moderationNote?: string;
  price: number;
  negotiable: boolean;
  exchangeAllowed: boolean;
  province: string;
  district?: string;
  addressText?: string;
  imageUrl?: string;
  images: string[];
  manufactureYear?: number;
  registrationYear?: number;
  mileageKm?: number;
  exteriorColor?: string;
  fuelType?: string;
  transmission?: string;
  engineCapacityCc?: number;
  rangeKm?: number;
  seats?: number;
  ownersCount?: number;
  origin?: string;
  viewCount: number;
  favoriteCount: number;
  publishedAt?: string;
  createdAt: string;
};
export type MarketplaceMessage = { publicId: string; senderId: number; senderName: string; messageType: "TEXT" | "IMAGE" | "VIDEO" | "DOCUMENT" | "LOCATION"; content?: string; attachmentUrl?: string; fileName?: string; mimeType?: string; latitude?: string; longitude?: string; read?: boolean; createdAt: string };
export type MarketplaceConversation = { publicId: string; listingPublicId: string; listingTitle: string; listingImageUrl?: string; listingPrice?: number; otherName: string; otherAvatarUrl?: string; lastMessage?: string; lastMessageAt?: string; unreadCount: number };
export type MarketplaceSellerReputation = {
  seller: { id: number; username: string; firstName?: string; lastName?: string; createdAt: string };
  summary: { averageRating: number; reviewCount: number };
  reviews: Array<{ publicId: string; rating: number; comment?: string; reviewerName: string; createdAt: string }>;
};
export type MarketplaceAppointment = {
  publicId: string;
  listingId: number;
  listingTitle: string;
  buyerId: number;
  sellerId: number;
  buyerName: string;
  sellerName: string;
  scheduledAt: string;
  location: string;
  note?: string;
  sellerNote?: string;
  status: "REQUESTED" | "CONFIRMED" | "DECLINED" | "COMPLETED" | "CANCELLED";
  createdAt: string;
  respondedAt?: string;
};
export type MarketplaceDraftAdvice = { title: string; description: string; suggestedPrice?: number; priceLow?: number; priceHigh?: number; completeness: number; missing: string[]; aiUsed: boolean };
export type MarketplaceVisionResult = { visionAvailable: boolean; title?: string; description?: string; categorySlug?: string; brand?: string; model?: string; variant?: string; manufactureYear?: number; registrationYear?: number; mileageKm?: number; exteriorColor?: string; fuelType?: string; transmission?: string; engineCapacityCc?: number; rangeKm?: number; seats?: number; origin?: string; condition?: string; confidence: number; detectedFeatures: string[]; imageFeedback: string[]; requiredFields: string[] };

export const marketplaceAPI = {
  categories: async () => (await apiClient.get<MarketplaceCategory[]>("/marketplace/categories")).data,
  listings: async () => (await apiClient.get<MarketplaceListing[]>("/marketplace/listings")).data,
  ownListings: async () => (await apiClient.get<MarketplaceListing[]>("/marketplace/seller/listings")).data,
  detail: async (publicId: string) => (await apiClient.get<MarketplaceListing>(`/marketplace/listings/${publicId}`)).data,
  editListing: async (id: string, payload: Record<string, unknown>) =>
    (await apiClient.post<MarketplaceListing>(`/marketplace/listings/${id}/edit`, payload)).data,
  removeListingImage: async (id: string, url: string) =>
    (await apiClient.post<MarketplaceListing>(`/marketplace/listings/${id}/remove-image`, { url })).data,
  withdrawListing: async (id: string) =>
    (await apiClient.post<MarketplaceListing>(`/marketplace/listings/${id}/withdraw`)).data,
  createListing: async (payload: Record<string, unknown>) =>
    (await apiClient.post<MarketplaceListing>("/marketplace/listings", payload)).data,
  uploadListingImage: async (listingId: string, file: File) => {
    const body = new FormData();
    body.append("file", file);
    return (await apiClient.post<{ id: number; url: string; mimeType: string; size: number }>(
      `/marketplace/listings/${listingId}/images`,
      body
    )).data;
  },
  publishListing: async (listingId: string) =>
    (await apiClient.post<MarketplaceListing>(`/marketplace/listings/${listingId}/publish`)).data,
  toggleFavorite: async (listingId: string) =>
    (await apiClient.post<{ saved: boolean }>(`/marketplace/listings/${listingId}/favorite`)).data,
  favoriteStatus: async (listingId: string) =>
    (await apiClient.get<{ favorite: boolean }>(`/marketplace/listings/${listingId}/favorite`)).data,
  favoriteListings: async () =>
    (await apiClient.get<MarketplaceListing[]>("/marketplace/favorites")).data,
  reportListing: async (listingId: string, reason: string) =>
    (await apiClient.post<{ reported: boolean }>(`/marketplace/listings/${listingId}/report`, { reason })).data,
  assistDraft: async (payload: Record<string, unknown>) =>
    (await apiClient.post<MarketplaceDraftAdvice>("/marketplace/advisor/draft", payload)).data,
  analyzeMedia: async (files: File[]) => {
    const body = new FormData(); files.slice(0, 4).forEach((file) => body.append("files", file));
    return (await apiClient.post<MarketplaceVisionResult>("/marketplace/advisor/analyze-media", body, { timeout: 50000 })).data;
  },
  openConversation: async (listingId: string) =>
    (await apiClient.post<{ publicId: string; listingTitle: string }>(`/marketplace/listings/${listingId}/conversation`)).data,
  conversationMessages: async (conversationId: string) =>
    (await apiClient.get<MarketplaceMessage[]>(`/marketplace/conversations/${conversationId}/messages`)).data,
  sendMessage: async (conversationId: string, content: string) =>
    (await apiClient.post<MarketplaceMessage>(`/marketplace/conversations/${conversationId}/messages`, { content })).data,
  sendAttachment: async (conversationId: string, file: File) => {
    const body = new FormData(); body.append("file", file);
    return (await apiClient.post<MarketplaceMessage>(`/marketplace/conversations/${conversationId}/attachments`, body)).data;
  },
  sendLocation: async (conversationId: string, latitude: number, longitude: number) =>
    (await apiClient.post<MarketplaceMessage>(`/marketplace/conversations/${conversationId}/location`, { latitude, longitude })).data,
  setTyping: async (conversationId: string, typing: boolean) => {
    await apiClient.post(`/marketplace/conversations/${conversationId}/typing`, { typing });
  },
  typingStatus: async (conversationId: string) =>
    (await apiClient.get<{ typing: boolean }>(`/marketplace/conversations/${conversationId}/typing`)).data,
  conversations: async () =>
    (await apiClient.get<MarketplaceConversation[]>("/marketplace/conversations")).data,
  createOffer: async (listingId: string, payload: { amount: number; depositAmount: number; message?: string }) =>
    (await apiClient.post<Record<string, unknown>>(`/marketplace/listings/${listingId}/offers`, payload)).data,
  myOffers: async () => (await apiClient.get<Array<Record<string, unknown>>>("/marketplace/offers/mine")).data,
  acceptOffer: async (offerId: string) =>
    (await apiClient.post<Record<string, unknown>>(`/marketplace/offers/${offerId}/accept`)).data,
  myTransactions: async () =>
    (await apiClient.get<Array<Record<string, unknown>>>("/marketplace/transactions/mine")).data,
  createSellerReview: async (transactionId: string, payload: { rating: number; comment?: string }) =>
    (await apiClient.post<Record<string, unknown>>(`/marketplace/transactions/${transactionId}/seller-review`, payload)).data,
  sellerReputation: async (sellerId: number) =>
    (await apiClient.get<MarketplaceSellerReputation>(`/marketplace/sellers/${sellerId}/reputation`)).data,
  createAppointment: async (listingId: string, payload: { scheduledAt: string; location: string; note?: string }) =>
    (await apiClient.post<MarketplaceAppointment>(`/marketplace/listings/${listingId}/appointments`, payload)).data,
  myAppointments: async () =>
    (await apiClient.get<MarketplaceAppointment[]>("/marketplace/appointments/mine")).data,
  updateAppointment: async (appointmentId: string, payload: { status: string; note?: string }) =>
    (await apiClient.patch<MarketplaceAppointment>(`/marketplace/appointments/${appointmentId}`, payload)).data,
  createPayment: async (transactionId: string, stage: "DEPOSIT" | "FINAL", provider: "VNPAY" | "BANK_TRANSFER" | "CASH_ON_DELIVERY") =>
    (await apiClient.post<Record<string, unknown>>(`/marketplace/transactions/${transactionId}/payments`, { stage, provider })).data,
  confirmMarketplacePayment: async (paymentId: string) =>
    (await apiClient.post<Record<string, unknown>>(`/marketplace/payments/${paymentId}/confirm`)).data,
  confirmHandover: async (transactionId: string) =>
    (await apiClient.post<Record<string, unknown>>(`/marketplace/transactions/${transactionId}/handover/confirm`)).data
};

export const formatMarketplacePrice = (price: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(price);

export const resolveMarketplaceMediaUrl = (url: string) =>
  url.startsWith("http") ? url : `${API_BASE_URL.replace(/\/api\/v1\/?$/, "")}${url}`;
