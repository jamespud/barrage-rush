package com.spud.barrage.auth.filter;

import com.spud.barrage.auth.dto.BarrageUserDetail;
import com.spud.barrage.auth.utils.JwtTokenUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Spud
 * @date 2025/3/16
 */
@Slf4j
public class TokenFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String token = JwtTokenUtils.getRequestToken(request);
    if (StringUtils.hasLength(token)) {
      BarrageUserDetail userDetail = JwtTokenUtils.checkAccessToken(token);
      if (userDetail != null) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(
            userDetail, null, userDetail.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
      }
    }
    chain.doFilter(request, response);
  }
}
