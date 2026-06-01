package com.jpoltramari.library_api.infrastructure.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter filter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(
            JwtAuthenticationFilter filter,
            @Qualifier("corsConfigurationSource")
            CorsConfigurationSource corsConfigurationSource
    ) {
        this.filter = filter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApiSecurityExceptionHandler handler
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource)
                )

                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(handler)
                        .accessDeniedHandler(handler)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()

                        .requestMatchers(HttpMethod.GET, "/books", "/books/**")
                            .hasAuthority("BOOK_READ")
                        .requestMatchers(HttpMethod.POST, "/books")
                            .hasAuthority("BOOK_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/books/**")
                            .hasAuthority("BOOK_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/books/**")
                            .hasAuthority("BOOK_DELETE")

                        .requestMatchers(HttpMethod.GET, "/authors", "/authors/**")
                            .hasAuthority("AUTHOR_READ")
                        .requestMatchers(HttpMethod.POST, "/authors")
                            .hasAuthority("AUTHOR_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/authors/**")
                            .hasAuthority("AUTHOR_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/authors/**")
                            .hasAuthority("AUTHOR_DELETE")

                        .requestMatchers(HttpMethod.GET, "/loans")
                            .hasAuthority("LOAN_READ_ALL")
                        .requestMatchers(HttpMethod.GET, "/loans/my")
                            .authenticated()
                        .requestMatchers(HttpMethod.POST, "/loans")
                            .hasAuthority("LOAN_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/approve")
                            .hasAuthority("LOAN_APPROVE")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/withdraw")
                            .hasAuthority("LOAN_WITHDRAW")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/return")
                            .hasAuthority("LOAN_RETURN")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/cancel")
                            .hasAuthority("LOAN_CANCEL")

                        .requestMatchers(HttpMethod.GET, "/books/*/copies/**")
                            .hasAuthority("BOOK_COPY_READ")
                        .requestMatchers(HttpMethod.POST, "/books/*/copies")
                            .hasAuthority("BOOK_COPY_CREATE")
                        .requestMatchers(HttpMethod.PUT, "/books/*/copies/**")
                            .hasAuthority("BOOK_COPY_UPDATE")
                        .requestMatchers(HttpMethod.PATCH, "/books/*/copies/**")
                            .hasAuthority("BOOK_COPY_UPDATE")
                        .requestMatchers(HttpMethod.DELETE, "/books/*/copies/**")
                            .hasAuthority("BOOK_COPY_DELETE")

                        .requestMatchers(HttpMethod.GET, "/users", "/users/**")
                            .hasAuthority("USER_ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        filter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
