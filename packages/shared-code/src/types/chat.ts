export interface Entity {
  type: string;
  value: string;
}

export interface Intent {
  name: string;
  confidence: number;
}

export interface Message {
  id: string;
  role: "user" | "assistant" | "system";
  content: string;
  createdAt: string;
}

export interface Conversation {
  id: string;
  messages: Message[];
}

export interface DialogueContext {
  sessionId: string;
  lastIntent?: Intent;
}

export interface ChatResponse {
  message: Message;
  suggestions?: string[];
}

export interface ChatbotRecommendation {
  id: number;
  name: string;
  slug: string;
  price: number;
  discountPrice?: number | null;
  stockQuantity: number;
  imageUrl?: string | null;
  categoryName?: string | null;
  reason: string;
}

export interface ChatbotAdvisorResponse {
  answer: string;
  matchedIntent: string;
  recommendations: ChatbotRecommendation[];
}
