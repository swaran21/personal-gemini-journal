package com.pm.personalgeminijournalbackend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;


@Configuration
@Profile("local")
public class LocalSecurityConfig {
    @Bean
    JwtDecoder localJwtDecoder(
            @Value("${app.security.oidc.issuer-uri}") String issuer,
            @Value("${app.security.oidc.jwk-set-uri}") String jwkSetUri,
            @Value("${app.security.oidc.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), audienceValidator(audience), subjectValidator()));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing", null));
    }

    static OAuth2TokenValidator<Jwt> subjectValidator() {
        return jwt -> jwt.getSubject() != null && jwt.getSubject().matches("[A-Za-z0-9._@-]{1,128}")
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Subject is invalid", null));
    }

    @Bean SecurityFilterChain localSecurityFilterChain(HttpSecurity http, UserRateLimitFilter rateLimitFilter) throws Exception {
        Converter<Jwt, AbstractAuthenticationToken> converter = jwt -> new UsernamePasswordAuthenticationToken(
                new FirebasePrincipal(jwt.getSubject()), jwt, RoleAuthorities.from(jwt.getClaim("realm_access"), jwt.getClaim("roles")));
        return http.csrf(csrf -> csrf.disable()).cors(cors -> {}).sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health/**").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/**").hasRole("USER").anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .addFilterAfter(rateLimitFilter, BearerTokenAuthenticationFilter.class).build();
    }
}
