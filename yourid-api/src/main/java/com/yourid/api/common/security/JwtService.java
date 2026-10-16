package com.yourid.api.common.security;

import com.yourid.api.common.exception.BadRequestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-token-expiration}")
    private Duration accessTokenExpiration;

    @Value("${app.jwt.max-session}")
    private Duration maxSession;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UserPrincipal principal) {
        return buildToken(principal, System.currentTimeMillis());
    }

    public String refreshAccessToken(UserPrincipal principal, String currentToken) {
        long authAt = extractAuthTime(currentToken);
        if (System.currentTimeMillis() - authAt > maxSession.toMillis()) {
            throw new BadRequestException("Session trop ancienne, veuillez vous reconnecter.");
        }
        return buildToken(principal, authAt);
    }

    private String buildToken(UserPrincipal principal, long authAtMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration.toMillis());

        return Jwts.builder()
                .subject(principal.getId().toString())
                .claim("email", principal.getUsername())
                .claim("role", principal.getUser().getRole().name())
                .claim("authAt", authAtMillis)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    private long extractAuthTime(String token) {
        Number authAt = extractClaim(token, c -> c.get("authAt", Number.class));
        if (authAt != null) return authAt.longValue();
        return extractClaim(token, Claims::getIssuedAt).getTime();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            UUID tokenUserId = extractUserId(token);
            UserPrincipal principal = (UserPrincipal) userDetails;
            boolean sameUser = tokenUserId.equals(principal.getId());
            return sameUser && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}