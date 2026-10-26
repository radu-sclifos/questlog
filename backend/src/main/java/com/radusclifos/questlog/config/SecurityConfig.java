package com.radusclifos.questlog.config;

import com.radusclifos.questlog.user.repository.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> users.findByUsername(username).map(user ->
                        org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                                .password(user.getPasswordHash()).roles("USER").disabled(!user.isEnabled()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // deliberately scoped: the future /games feature keeps its existing behavior.
        http.securityMatcher("/auth/**", "/me", "/users/**", "/entries", "/entries/**")
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/auth/csrf", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/entries").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .requestCache(RequestCacheConfigurer::disable)
                .formLogin(f -> f.loginProcessingUrl("/auth/login")
                        .successHandler((req, res, auth) -> res.setStatus(204))
                        .failureHandler((req, res, err) -> res.setStatus(401)))
                .logout(l -> l.logoutUrl("/auth/logout").logoutSuccessHandler((req, res, auth) -> res.setStatus(204)));
        return http.build();
    }

}
