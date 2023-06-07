package com.ting.ting.configuration;

import com.ting.ting.configuration.filter.JwtTokenFilter;
import com.ting.ting.exception.CustomAuthenticationEntryPoint;
import com.ting.ting.service.UserService;
import com.ting.ting.util.JwtTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Configuration
public class SecurityConfig {

    private final UserService userService;
    private final JwtTokenGenerator jwtTokenGenerator;

    public SecurityConfig(UserService userService, JwtTokenGenerator jwtTokenGenerator) {
        this.userService = userService;
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .mvcMatchers("/ting/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(new JwtTokenFilter(userService, jwtTokenGenerator), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                .and()
                .build();
    }
}