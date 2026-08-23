package com.payment.platform.config;

import com.payment.platform.security.ApiSignFilter;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiSignFilter apiSignFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                    "/api/wechat/notify",
                    "/api/wechat/oauth",
                    "/api/wechat/oauth/authorize",
                    "/api/open/**",
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
                .requestMatchers("/api/order/admin/**").hasAuthority("ROLE_ADMIN")
                // 其他接口需登录
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiSignFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
