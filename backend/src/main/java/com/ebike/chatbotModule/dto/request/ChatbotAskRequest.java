package com.ebike.chatbotModule.dto.request;

public record ChatbotAskRequest(
    String message,
    String content,
    String chatId,
    String page,
    String currentVehicleId,
    String currentUserId,
    String conversationId
) {
    public ChatbotAskRequest(String message, String content, String chatId) { this(message, content, chatId, null, null, null, null); }
    public String effectiveMessage() {
        if (message != null && !message.isBlank()) {
            return message;
        }
        return content;
    }
}
