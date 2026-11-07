package com.ebike.authModule.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ebike.authModule.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;

class AccountSecurityTest {
    @Test void smtpFailureDoesNotRevealPasswordResetAccountExistence() {
        var jdbc=mock(JdbcTemplate.class);
        var delivery=mock(AccountCodeDelivery.class);
        var encoder=mock(PasswordEncoder.class);
        when(jdbc.queryForList(anyString(),eq("known@example.test"))).thenReturn(List.of(Map.of("id",9L,"email","known@example.test")));
        when(jdbc.queryForObject(contains("purpose=?"),eq(Integer.class),eq(9L),eq("RESET_PASSWORD"))).thenReturn(0);
        when(jdbc.queryForObject(contains("1 hour"),eq(Integer.class),eq(9L))).thenReturn(0);
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(delivery).sendEmail(eq("known@example.test"),anyString(),eq(true));
        var service=new AccountVerificationService(jdbc,encoder,delivery);
        assertTrue(service.request(null,"known@example.test","RESET_PASSWORD").containsKey("challengeId"));
        verifyNoInteractions(encoder);
    }
    @Test void emptyProviderSettingsDisableDeliveryWithoutPreventingConstruction() {
        var mail=mock(JavaMailSender.class);
        var delivery=new AccountCodeDelivery(mail,new RestTemplateBuilder(),"","no-reply@motionx.local","","","");
        assertFalse(delivery.emailAvailable());assertFalse(delivery.phoneAvailable());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->delivery.sendEmail("test@example.test","123456",false)).getStatusCode().value());
        assertEquals(503,assertThrows(ResponseStatusException.class,()->delivery.sendPhone("0901234567")).getStatusCode().value());
        verifyNoInteractions(mail);
    }
    @Test void phoneNormalizationDoesNotAcceptArbitraryInternationalTargets() {
        assertEquals("+84901234567",AccountCodeDelivery.normalizePhone("0901 234 567"));
        assertEquals("+84901234567",AccountCodeDelivery.normalizePhone("+84 901234567"));
        assertThrows(ResponseStatusException.class,()->AccountCodeDelivery.normalizePhone("+15551234567"));
        assertThrows(ResponseStatusException.class,()->AccountCodeDelivery.normalizePhone(null));
    }
    @Test void resetPasswordRespectsBcryptUtf8Limit() {
        assertDoesNotThrow(()->AccountVerificationService.validatePassword("ValidPassword123!"));
        assertThrows(ResponseStatusException.class,()->AccountVerificationService.validatePassword("short"));
        assertThrows(ResponseStatusException.class,()->AccountVerificationService.validatePassword("密".repeat(25)));
    }
    @Test void passwordResetVersionRevokesPreviouslySignedTokens() {
        var provider=new JwtTokenProvider();
        ReflectionTestUtils.setField(provider,"jwtSecret","test-only-signing-secret-at-least-32-characters");
        ReflectionTestUtils.setField(provider,"jwtExpirationMs",3600000L);
        var user=new User();user.setId(99L);user.setUsername("security-test");user.setEmail("security@example.test");
        String token=provider.generateToken(user);
        assertTrue(provider.validateToken(token));assertTrue(provider.matchesTokenVersion(token,user));
        user.setTokenVersion(1);
        assertFalse(provider.matchesTokenVersion(token,user));
        assertTrue(provider.matchesTokenVersion(provider.generateToken(user),user));
    }
}
