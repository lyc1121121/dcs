package com.dcsmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 108단계: 로그인 인증을 제거한다(내부망에서만 접근 가능하므로 인증 없이 바로 진입).
 * CSRF 보호는 그대로 유지한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests(auth -> auth.anyRequest().permitAll());
        // 158단계 후속: PRIVATE 파일탐색기에서 PDF를 iframe으로 미리보기 하려면
        // 기본값(DENY) 대신 같은 출처(SAMEORIGIN)에서는 프레임 삽입을 허용해야 한다.
        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }
}
