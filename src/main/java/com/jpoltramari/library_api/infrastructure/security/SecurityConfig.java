package com.jpoltramari.library_api.infrastructure.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter filter;

    public SecurityConfig(JwtAuthenticationFilter filter){
        this.filter = filter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ApiSecurityExceptionHandler apiSecurityExceptionHandler) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(apiSecurityExceptionHandler)
                        .accessDeniedHandler(apiSecurityExceptionHandler)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST,"/users").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/books/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/books/**").hasAuthority("CREATE_BOOK")
                        .requestMatchers(HttpMethod.PUT, "/books/**").hasAuthority("CREATE_BOOK")
                        .requestMatchers(HttpMethod.DELETE, "/books/**").hasAuthority("CREATE_BOOK")

                        .requestMatchers(HttpMethod.GET, "/authors/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/authors/**").hasAuthority("CREATE_AUTHOR")
                        .requestMatchers(HttpMethod.PUT, "/authors/**").hasAuthority("CREATE_AUTHOR")
                        .requestMatchers(HttpMethod.DELETE, "/authors/**").hasAuthority("CREATE_AUTHOR")

                        .requestMatchers(HttpMethod.POST, "/loans").hasAuthority("REQUEST_LOAN")
                        .requestMatchers(HttpMethod.GET, "/loans/**").hasAuthority("VIEW_ALL_LOANS")

                        .requestMatchers(HttpMethod.PUT, "/loans/*/approve").hasAuthority("APPROVE_LOAN")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/withdraw").hasAuthority("REGISTER_WITHDRAWAL")
                        .requestMatchers(HttpMethod.PUT, "/loans/*/return").hasAuthority("REGISTER_RETURN")

                        .requestMatchers(HttpMethod.GET, "/users/**").hasAuthority("VIEW_ALL_USERS")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }
}