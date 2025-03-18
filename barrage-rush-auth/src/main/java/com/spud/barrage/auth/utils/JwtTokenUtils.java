package com.spud.barrage.auth.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spud.barrage.auth.dto.BarrageUserDetail;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * @author spud
 * @date 2024/11/8
 */
@Slf4j
public class JwtTokenUtils {

  private static final String SECRET = "secret";

  private static final SecretKey key = Jwts.SIG.HS256.key().build();

  // access token: 30 minutes
  private static final int ACCESS_TOKEN_TTL = 30 * 60 * 1000;

  // refresh token: 7 days
  private static final int REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60 * 1000;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static BarrageUserDetail checkAccessToken(String token) {
    Date now = new Date();
    Jws<Claims> claimsJws = Jwts.parser()
        .verifyWith(key)
        .build().parseSignedClaims(token);
    Claims payload = claimsJws.getPayload();
    Date expiration = payload.getExpiration();
    if (expiration.before(now)) {
      log.warn("token expired");
      return null;
    }
    BarrageUserDetail detail = OBJECT_MAPPER.convertValue(payload.get("userDetail"),
        BarrageUserDetail.class);
    List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
    detail.getBannedRooms().forEach(room -> {
      grantedAuthorities.add(
          new SimpleGrantedAuthority("ROOM_" + room.getFirst() + ":" + room.getSecond()));
    });
    detail.setBannedRoomsAuthorities(grantedAuthorities);
    return detail;
  }
  
  public static String generateAccessToken(BarrageUserDetail userDetail) throws JsonProcessingException {
    Date exp = new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL);
    return Jwts.builder()
        .claim("userDetail", userDetail)
        .issuedAt(new Date())
        .expiration(exp)
        .signWith(key)
        .claim("userDetail", OBJECT_MAPPER.writeValueAsString(userDetail))
        .compact();
  }

  public static String generateRefreshToken(BarrageUserDetail userDetail) throws JsonProcessingException {
    Date exp = new Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL);
    return Jwts.builder()
        .claim("userDetail", userDetail)
        .issuedAt(new Date())
        .expiration(exp)
        .signWith(key)
        .claim("userDetail", OBJECT_MAPPER.writeValueAsString(userDetail))
        .compact();
  }

  public static String getRequestToken(HttpServletRequest request) {
    return request.getHeader("Authorization");
  }
}
