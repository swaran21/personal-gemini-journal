package com.pm.personalgeminijournalbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
public class RateLimitConfig {
    @Bean
    UserRateLimitFilter userRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.journal-writes-per-hour:30}") int journalWrites,
            @Value("${app.rate-limit.rag-queries-per-hour:20}") int ragQueries,
            @Value("${app.rate-limit.api-requests-per-minute:120}") int apiRequests) {
        return new UserRateLimitFilter(objectMapper, journalWrites, ragQueries, apiRequests);
    }

    /** The filter belongs only to Spring Security, after authentication has established the UID. */
    @Bean
    FilterRegistrationBean<UserRateLimitFilter> disableContainerRegistration(UserRateLimitFilter filter) {
        FilterRegistrationBean<UserRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
