package com.payment.platform.config;

import com.payment.platform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/sms/send",
                    "/api/auth/refresh",
                    "/api/auth/captcha/**",
                    "/api/alipay/notify",
                    "/api/alipay/return",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/doc.html",
                    "/webjars/**"
                ).permitAll()
                // 收银台接口（无需登录）
                .requestMatchers(HttpMethod.GET, "/api/qrcode/info").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/order/status").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/order/create").permitAll()
                // 管理端接口
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/statistics/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers("/api/merchant/admin/**").hasAuthority("ROLE_ADMIN")
                // 其他接口需登录
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
