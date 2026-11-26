package com.jpoltramari.library_api.infrastructure.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String BOOK_COPIES_PATH = "/books/*/copies/**";
    private static final String BOOKS_PATH = "/books/**";
    private static final String AUTHORS_PATH = "/authors/**";

    private final JwtAuthenticationFilter filter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${app.security.refresh-cookie-secure:true}")
    private boolean refreshCookieSecure;
    @Value("${app.security.refresh-cookie-same-site:Lax}")
    private String refreshCookieSameSite;

    public SecurityConfig(JwtAuthenticationFilter filter, @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) {
        this.filter = filter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite));
        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiSecurityExceptionHandler handler,
            CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        return http
                // Require CSRF tokens only where the browser automatically sends the refresh cookie.
                // All other API endpoints remain stateless and authenticate with Bearer tokens.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(request -> {
                            String path = request.getRequestURI();
                            return "POST".equalsIgnoreCase(request.getMethod())
                                    && (path.endsWith("/auth/refresh") || path.endsWith("/auth/logout"));
                        }))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(handler).accessDeniedHandler(handler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()

                        .requestMatchers(HttpMethod.GET, BOOK_COPIES_PATH).hasAuthority("BOOK_COPY_READ")
                        .requestMatchers(HttpMethod.POST, "/books/*/copies").hasAuthority("BOOK_COPY_CREATE")
                        .requestMatchers(HttpMethod.PUT, BOOK_COPIES_PATH).hasAuthority("BOOK_COPY_UPDATE")
                        .requestMatchers(HttpMethod.PATCH, BOOK_COPIES_PATH).hasAuthority("BOOK_COPY_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, BOOK_COPIES_PATH).hasAuthority("BOOK_COPY_DELETE")

                        .requestMatchers(HttpMethod.GET, "/books", BOOKS_PATH).hasAuthority("BOOK_READ")
                        .requestMatchers(HttpMethod.POST, "/books").hasAuthority("BOOK_CREATE")
                        .requestMatchers(HttpMethod.PUT, BOOKS_PATH).hasAuthority("BOOK_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, BOOKS_PATH).hasAuthority("BOOK_DELETE")

                        .requestMatchers(HttpMethod.GET, "/authors", AUTHORS_PATH).hasAuthority("AUTHOR_READ")
                        .requestMatchers(HttpMethod.POST, "/authors").hasAuthority("AUTHOR_CREATE")
                        .requestMatchers(HttpMethod.PUT, AUTHORS_PATH).hasAuthority("AUTHOR_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, AUTHORS_PATH).hasAuthority("AUTHOR_DELETE")

                        .requestMatchers(HttpMethod.GET, "/loans/users").hasAuthority("LOAN_CREATE_FOR_USER")
                        .requestMatchers(HttpMethod.GET, "/loans").hasAuthority("LOAN_READ_ALL")
                        .requestMatchers(HttpMethod.GET, "/loans/my").authenticated()
                        .requestMatchers(HttpMethod.GET, "/loans/*").hasAuthority("LOAN_READ_ALL")
                        .requestMatchers(HttpMethod.POST, "/loans/for-user").hasAuthority("LOAN_CREATE_FOR_USER")
                        .requestMatchers(HttpMethod.POST, "/loans").hasAuthority("LOAN_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/approve").hasAuthority("LOAN_APPROVE")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/return").hasAuthority("LOAN_RETURN")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/cancel").hasAnyAuthority("LOAN_CANCEL", "LOAN_CANCEL_ANY")

                        .requestMatchers(HttpMethod.GET, "/fines/my").hasAuthority("FINE_READ_OWN")
                        .requestMatchers(HttpMethod.GET, "/fines").hasAuthority("FINE_READ_ALL")
                        .requestMatchers(HttpMethod.GET, "/fines/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/fines/*/waive").hasAuthority("FINE_WAIVE")

                        .requestMatchers(HttpMethod.GET, "/users", "/users/**").hasAuthority("USER_ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
