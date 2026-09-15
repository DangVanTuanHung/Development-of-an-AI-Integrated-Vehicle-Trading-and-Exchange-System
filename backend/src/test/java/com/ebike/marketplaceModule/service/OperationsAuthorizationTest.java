package com.ebike.marketplaceModule.service;
import com.ebike.marketplaceModule.controller.MarketplaceOperationsController;
import com.ebike.marketplaceModule.controller.MarketplaceCatalogController;
import com.ebike.adminModule.repository.AdminAuditLogRepository;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationsAuthorizationTest {
 private AnnotationConfigApplicationContext context;
 @Configuration @EnableMethodSecurity static class Config {
  @Bean MarketplaceOperationsService service(){return mock(MarketplaceOperationsService.class);}
  @Bean MarketplaceOperationsController operations(MarketplaceOperationsService s){return new MarketplaceOperationsController(s);}
  @Bean MarketplaceCatalogController catalog(){return new MarketplaceCatalogController(mock(JdbcTemplate.class),mock(AdminAuditLogRepository.class));}
 }
 @BeforeEach void setup(){context=new AnnotationConfigApplicationContext(Config.class);}
 @AfterEach void close(){SecurityContextHolder.clearContext();context.close();}
 private void as(String role){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("tester",null,AuthorityUtils.createAuthorityList("ROLE_"+role)));}
 @Test void customerCannotAccessOperations(){as("CUSTOMER");assertThrows(AccessDeniedException.class,()->context.getBean(MarketplaceOperationsController.class).overview());}
 @Test void managerCanOperateButCannotManageCatalog(){as("MANAGER");assertDoesNotThrow(()->context.getBean(MarketplaceOperationsController.class).overview());assertThrows(AccessDeniedException.class,()->context.getBean(MarketplaceCatalogController.class).list("brands"));}
 @Test void adminCanAccessBoth(){as("ADMIN");assertDoesNotThrow(()->context.getBean(MarketplaceOperationsController.class).overview());assertDoesNotThrow(()->context.getBean(MarketplaceCatalogController.class).list("brands"));}
 @Test void legacySupportRoleIsNotAnAdministrator(){as("SUPPORT");assertThrows(AccessDeniedException.class,()->context.getBean(MarketplaceCatalogController.class).list("brands"));}
}
