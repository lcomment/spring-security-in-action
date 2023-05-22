package com.ch6.ssia.global.config;

import com.ch6.ssia.domain.member.service.MemberDetailsService;
import com.ch6.ssia.global.security.AuthenticationProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final MemberDetailsService memberDetailsService;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .formLogin((login) ->
                        login.defaultSuccessUrl("/main", true)
                )
                .authorizeHttpRequests((auth) ->
                        auth.anyRequest().authenticated()
                )
                .authenticationProvider(new AuthenticationProviderService(
                        memberDetailsService,
                        bCryptPasswordEncoder(),
                        sCryptPasswordEncoder()))
                .build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SCryptPasswordEncoder sCryptPasswordEncoder() {
        return new SCryptPasswordEncoder(16384, 8, 1, 32, 64);
    }
}
