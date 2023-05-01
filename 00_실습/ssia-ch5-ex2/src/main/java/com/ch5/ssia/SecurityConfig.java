package com.ch5.ssia;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(c -> {
                    c.realmName("OTHER");
                    c.authenticationEntryPoint(new CustomEntryPoint());
                })
                .authorizeHttpRequests().anyRequest().authenticated()
                .and()
                .build();
    }

}
