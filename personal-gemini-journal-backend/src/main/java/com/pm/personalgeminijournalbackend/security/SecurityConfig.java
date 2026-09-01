package com.pm.personalgeminijournalbackend.security;

import com.pm.personalgeminijournalbackend.config.ApplicationConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration @EnableWebSecurity
@Profile("cloud")
public class SecurityConfig {
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, FirebaseAuthenticationFilter firebaseFilter, UserRateLimitFilter rateLimitFilter) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/actuator/health/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(firebaseFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, FirebaseAuthenticationFilter.class).build();
    }
}
