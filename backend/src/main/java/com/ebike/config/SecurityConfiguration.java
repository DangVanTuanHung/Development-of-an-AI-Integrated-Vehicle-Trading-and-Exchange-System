package com.ebike.config;

import com.ebike.authModule.filter.JwtAuthenticationFilter;
import com.ebike.shared.constants.PermissionConstants;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    private static final String[] ADMIN_CONSOLE_ROLES = {"ADMIN"};

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .anonymous(anonymous -> anonymous.authorities(
                PermissionConstants.Guest.PRODUCT_VIEW,
                PermissionConstants.Guest.PRODUCT_SEARCH,
                PermissionConstants.Guest.CATEGORY_VIEW,
                PermissionConstants.Guest.REVIEW_VIEW,
                PermissionConstants.Guest.ORDER_CREATE,
                PermissionConstants.Guest.PAYMENT_CREATE,
                PermissionConstants.ChatbotManagement.CHATBOT_USE
            ))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/marketplace/**", "/manager/support-tickets/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/orders/quote").permitAll()
                .requestMatchers(HttpMethod.POST, "/support/tickets").permitAll()
                .requestMatchers(HttpMethod.GET, "/support/tickets/mine").authenticated()
                .requestMatchers(HttpMethod.POST, "/orders/email-verification/send", "/orders/email-verification/verify").permitAll()
                .requestMatchers("/payments/vnpay/ipn", "/payments/vnpay/return").permitAll()
                .requestMatchers(
                    "/auth/register",
                    "/auth/login",
                    "/auth/login/**",
                    "/auth/logout",
                    "/auth/session",
                    "/health/**",
                    "/media/**",
                    "/error"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/products/**").hasAuthority(PermissionConstants.Guest.PRODUCT_VIEW)
                .requestMatchers(HttpMethod.GET, "/marketplace/favorites", "/marketplace/listings/*/favorite").authenticated()
                .requestMatchers(HttpMethod.GET, "/marketplace/categories", "/marketplace/listings/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/marketplace/seller/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/marketplace/listings").authenticated()
                .requestMatchers(HttpMethod.POST, "/marketplace/listings/*/images").authenticated()
                .requestMatchers(HttpMethod.POST, "/marketplace/listings/*/publish", "/marketplace/listings/*/edit", "/marketplace/listings/*/withdraw", "/marketplace/listings/*/remove-image").authenticated()
                .requestMatchers(HttpMethod.POST, "/marketplace/listings/*/favorite", "/marketplace/listings/*/report").authenticated()
                .requestMatchers(HttpMethod.POST, "/marketplace/advisor/draft", "/marketplace/advisor/analyze-media").authenticated()
                .requestMatchers("/marketplace/conversations/**", "/marketplace/listings/*/conversation").authenticated()
                .requestMatchers("/marketplace/offers/**", "/marketplace/transactions/**", "/marketplace/payments/**", "/marketplace/listings/*/offers").authenticated()
                .requestMatchers(HttpMethod.POST, "/products/*/reviews").authenticated()
                .requestMatchers(HttpMethod.PUT, "/reviews/*").hasAnyAuthority(
                    PermissionConstants.Customer.REVIEW_CREATE,
                    PermissionConstants.ProductManagement.REVIEW_MODERATE
                )
                .requestMatchers(HttpMethod.DELETE, "/reviews/*").hasAnyAuthority(
                    PermissionConstants.Customer.REVIEW_CREATE,
                    PermissionConstants.ProductManagement.REVIEW_MODERATE
                )
                .requestMatchers(HttpMethod.GET, "/showrooms").hasAuthority(PermissionConstants.Guest.PRODUCT_VIEW)
                .requestMatchers(HttpMethod.POST, "/chatbot/ask").hasAuthority(PermissionConstants.ChatbotManagement.CHATBOT_USE)
                .requestMatchers(HttpMethod.POST, "/marketplace/advisor/ask").hasAuthority(PermissionConstants.ChatbotManagement.CHATBOT_USE)
                .requestMatchers(HttpMethod.POST, "/chatbot/debug").hasAuthority(PermissionConstants.ChatbotManagement.CHATBOT_CONFIGURE)
                .requestMatchers(HttpMethod.GET, "/auth/profile").authenticated()
                .requestMatchers(HttpMethod.PUT, "/auth/profile").authenticated()
                .requestMatchers("/auth/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/orders", "/orders/*").hasAnyAuthority(
                    PermissionConstants.Customer.ORDER_VIEW_OWN,
                    PermissionConstants.OrderManagement.ORDER_VIEW_ALL
                )
                .requestMatchers(HttpMethod.POST, "/orders").hasAuthority(PermissionConstants.Guest.ORDER_CREATE)
                .requestMatchers(HttpMethod.POST, "/orders/*/cancellation-request").hasAuthority(PermissionConstants.Customer.ORDER_CANCEL_OWN)
                .requestMatchers(HttpMethod.PATCH, "/orders/*/status").hasAuthority(PermissionConstants.OrderManagement.ORDER_UPDATE_STATUS)
                .requestMatchers(HttpMethod.GET, "/favorites").hasAuthority(PermissionConstants.Customer.FAVORITE_VIEW)
                .requestMatchers(HttpMethod.POST, "/favorites").hasAuthority(PermissionConstants.Customer.FAVORITE_UPDATE)
                .requestMatchers(HttpMethod.DELETE, "/favorites/*").hasAuthority(PermissionConstants.Customer.FAVORITE_UPDATE)
                .requestMatchers(HttpMethod.GET, "/notifications", "/notifications/unread-count", "/notifications/stream").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/notifications/*/read", "/notifications/read-all").authenticated()
                .requestMatchers(HttpMethod.GET, "/payments/history").hasAnyAuthority(
                    PermissionConstants.Customer.PAYMENT_VIEW_OWN,
                    PermissionConstants.Customer.ORDER_VIEW_OWN,
                    PermissionConstants.OrderManagement.ORDER_VIEW_ALL
                )
                .requestMatchers(HttpMethod.POST, "/payments/vnpay/create").hasAuthority(PermissionConstants.Guest.PAYMENT_CREATE)
                .requestMatchers(HttpMethod.GET, "/users/**").hasAnyAuthority(
                    PermissionConstants.Customer.PROFILE_VIEW,
                    PermissionConstants.UserManagement.USER_VIEW
                )
                .requestMatchers(HttpMethod.POST, "/users/**").hasAuthority(PermissionConstants.Customer.PROFILE_UPDATE)
                .requestMatchers(HttpMethod.PUT, "/users/**").hasAuthority(PermissionConstants.Customer.PROFILE_UPDATE)
                .requestMatchers(HttpMethod.GET,
                    "/admin/overview",
                    "/admin/pricing-rules",
                    "/admin/promotions",
                    "/admin/accounts",
                    "/admin/roles",
                    "/admin/audit-logs"
                    , "/admin/support-tickets"
                ).hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.PATCH, "/admin/support-tickets/*").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.PATCH, "/admin/pricing-rules/*").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.POST, "/admin/promotions").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.PATCH, "/admin/promotions/*").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.POST, "/admin/accounts").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.PATCH, "/admin/accounts/*/role").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.PATCH, "/admin/accounts/*/status").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.DELETE, "/admin/accounts/*").hasAnyRole(ADMIN_CONSOLE_ROLES)
                .requestMatchers(HttpMethod.POST, "/admin/product-images").hasAuthority(PermissionConstants.ProductManagement.PRODUCT_CREATE)
                .requestMatchers(HttpMethod.GET, "/admin/product-images/**").hasAuthority(PermissionConstants.ProductManagement.PRODUCT_UPDATE)
                .requestMatchers(HttpMethod.PUT, "/admin/product-images/**").hasAuthority(PermissionConstants.ProductManagement.PRODUCT_UPDATE)
                .requestMatchers(HttpMethod.DELETE, "/admin/product-images/**").hasAuthority(PermissionConstants.ProductManagement.PRODUCT_DELETE)
                .requestMatchers("/customer/**").hasAnyAuthority(
                    PermissionConstants.Customer.PROFILE_VIEW,
                    PermissionConstants.OrderManagement.ORDER_VIEW_ALL
                )
                .requestMatchers("/manager/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasAuthority(PermissionConstants.AccessControl.PERMISSION_MANAGE)
                .anyRequest().denyAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Unauthorized\"}");
                })
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
