package com.ting.ting.util;

import com.ting.ting.exception.ErrorCode;
import com.ting.ting.exception.ServiceType;
import com.ting.ting.exception.TingApplicationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTokenGenerator {

    private final Key key;

    public JwtTokenGenerator(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createTokenById(Long id) {
        Map<String, Object> payloads = new HashMap<>();
        payloads.put("id", id);
        Date now = new Date();
        Date expiration = new Date(now.getTime() + Duration.ofDays(1).toMillis());
        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE)
                .setClaims(payloads)
                .setExpiration(expiration)
                .setSubject("user-auto")
                .signWith(key)
                .compact();
    }

    public boolean validToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(this.key).build().parseClaimsJws(token).getBody();
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            throw new TingApplicationException(ErrorCode.TOKEN_ERROR, ServiceType.UTIL, "Invalid JWT signature.");
        } catch (ExpiredJwtException e) {
            throw new TingApplicationException(ErrorCode.TOKEN_ERROR, ServiceType.UTIL, "Expired JWT token.");
        } catch (UnsupportedJwtException e) {
            throw new TingApplicationException(ErrorCode.TOKEN_ERROR, ServiceType.UTIL, "Unsupported JWT token.");
        } catch (IllegalArgumentException e) {
            throw new TingApplicationException(ErrorCode.TOKEN_ERROR, ServiceType.UTIL, "Invalid JWT token");
        }
    }

    public Long getIdByToken(String token) {
        Claims body = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return body.get("id", Long.class);
    }
}
