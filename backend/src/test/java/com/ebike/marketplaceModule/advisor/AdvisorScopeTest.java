package com.ebike.marketplaceModule.advisor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ebike.marketplaceModule.service.MarketplaceAdvisorService;
import com.ebike.chatbotModule.service.GeminiChatClient;
import com.ebike.chatbotModule.dto.request.ChatbotAskRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;

class AdvisorScopeTest {
    private final AdvisorIntentClassifier classifier = new AdvisorIntentClassifier();

    @Test void unrelatedQuestionsNeverBecomeFollowUps() {
        for (boolean context : new boolean[]{false, true}) {
            for (String question : new String[]{"ronaldo hay messi ai hay hơn", "so sánh Ronaldo và Messi", "thời tiết hôm nay", "xin công thức nấu ăn", "abcxyz", "so sanh iPhone va Samsung", "nen mua laptop nao"}) {
                assertEquals(AdvisorIntent.UNKNOWN, classifier.classify(question, context).intent(), question);
            }
        }
    }

    @Test void meaningfulFollowUpsStillWork() {
        for (String question : new String[]{"500 triệu", "rẻ hơn nữa", "đời cao hơn", "xe thứ 2", "cho tôi xem hình ảnh", "ok"}) {
            assertEquals(AdvisorIntent.FOLLOW_UP, classifier.classify(question, true).intent(), question);
        }
        assertEquals(AdvisorIntent.VEHICLE_COMPARE, classifier.classify("so sánh Vios và Accent", false).intent());
        assertEquals(AdvisorIntent.FINANCE, classifier.classify("có trả góp không", true).intent());
    }

    @Test void offTopicDoesNotQueryVehiclesOrAiEvenWithBudgetAndPageContext() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        GeminiChatClient gemini = mock(GeminiChatClient.class);
        MarketplaceAdvisorService service = new MarketplaceAdvisorService(jdbc, gemini, new ObjectMapper(), classifier);
        service.ask(new ChatbotAskRequest("tôi muốn mua xe", null, "scope-test"));
        service.ask(new ChatbotAskRequest("500 triệu", null, "scope-test"));
        clearInvocations(jdbc, gemini);
        var response = service.ask(new ChatbotAskRequest("ronaldo hay messi ai hay hơn", null, "scope-test", null, "vehicle-id", null, null));
        assertEquals("out_of_scope", response.matchedIntent());
        assertTrue(response.recommendations().isEmpty());
        verifyNoInteractions(jdbc, gemini);
        var followUp = service.ask(new ChatbotAskRequest("500 triệu", null, "scope-test"));
        assertEquals("follow_up", followUp.matchedIntent());
    }

    @Test void emptyInputDoesNotReachTools() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        GeminiChatClient gemini = mock(GeminiChatClient.class);
        var service = new MarketplaceAdvisorService(jdbc, gemini, new ObjectMapper(), classifier);
        assertEquals("marketplace_clarification", service.ask(null).matchedIntent());
        verifyNoInteractions(jdbc, gemini);
    }
}
