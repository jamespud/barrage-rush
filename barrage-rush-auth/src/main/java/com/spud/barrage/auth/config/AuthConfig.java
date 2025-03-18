package com.spud.barrage.auth.config;

import com.spud.barrage.auth.filter.TokenFilter;
import com.spud.barrage.auth.service.AnchorUserDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class AuthConfig {
  
  @Autowired
  AnchorUserDetailService userDetailService;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorizeRequests -> {
          authorizeRequests.requestMatchers("/").permitAll();
          authorizeRequests.requestMatchers("/sys/login").permitAll();
          authorizeRequests.requestMatchers("/sys/logout").permitAll();
          authorizeRequests.anyRequest().authenticated();
        });
//        http.logout(logout -> logout.logoutSuccessHandler())
    http.addFilterBefore(jwtAuthenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  public TokenFilter jwtAuthenticationTokenFilter() {
    return new TokenFilter();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(userDetailService);
    return new ProviderManager(daoAuthenticationProvider);
  }

}
