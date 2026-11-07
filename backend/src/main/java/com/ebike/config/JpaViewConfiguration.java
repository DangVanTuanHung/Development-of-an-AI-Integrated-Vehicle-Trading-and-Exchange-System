package com.ebike.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Preserve view loading for existing REST controllers, but never bind an
 * EntityManager to the lifetime of a long-running notification stream. */
@Configuration
@ConditionalOnProperty(prefix="spring.jpa",name="open-in-view",havingValue="true",matchIfMissing=true)
public class JpaViewConfiguration implements WebMvcConfigurer {
    private final EntityManagerFactory factory;
    public JpaViewConfiguration(EntityManagerFactory factory){this.factory=factory;}
    @Bean
    public OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor(){
        var interceptor=new OpenEntityManagerInViewInterceptor();
        interceptor.setEntityManagerFactory(factory);
        return interceptor;
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addWebRequestInterceptor(openEntityManagerInViewInterceptor())
            .excludePathPatterns("/notifications/stream");
    }
}
