package com.ecru.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

    @Value("${jwt.secret:ecru-secret-key-2026-for-jwt-token-generation}")
    private String secret;

    @Value("${jwt.access-token-expire:7200}")
    private Long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:604800}")
    private Long refreshTokenExpire;

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    // Token黑名单（实际生产环境应使用Redis）
    private final Set<String> tokenBlacklist = ConcurrentHashMap.newKeySet();

    public String generateAccessToken(Long userId, String username) {
        return generateToken(userId, username, TOKEN_TYPE_ACCESS, accessTokenExpire);
    }

    public String generateRefreshToken(Long userId, String username) {
        return generateToken(userId, username, TOKEN_TYPE_REFRESH, refreshTokenExpire);
    }

    private String generateToken(Long userId, String username, String tokenType, Long expireSeconds) {
        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expireSeconds * 1000);

        return JWT.create()
                .withClaim(CLAIM_USER_ID, userId)
                .withClaim(CLAIM_USERNAME, username)
                .withClaim(CLAIM_TOKEN_TYPE, tokenType)
                .withIssuedAt(now)
                .withExpiresAt(expireTime)
                .sign(Algorithm.HMAC256(secret));
    }

    public DecodedJWT verifyToken(String token) throws JWTVerificationException {
        if (isTokenBlacklisted(token)) {
            throw new JWTVerificationException("Token已失效");
        }
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .build();
        return verifier.verify(token);
    }

    public Long getUserId(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim(CLAIM_USER_ID).asLong();
    }

    public String getUsername(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim(CLAIM_USERNAME).asString();
    }

    public String getTokenType(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getClaim(CLAIM_TOKEN_TYPE).asString();
    }

    public Date getExpiresAt(String token) {
        DecodedJWT jwt = JWT.decode(token);
        return jwt.getExpiresAt();
    }

    /**
     * 将Token加入黑名单（登出时使用）
     */
    public void blacklistToken(String token) {
        tokenBlacklist.add(token);
    }

    /**
     * 检查Token是否在黑名单中
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklist.contains(token);
    }

    /**
     * 刷新AccessToken
     */
    public Map<String, Object> refreshAccessToken(String refreshToken) {
        DecodedJWT decodedJWT = verifyToken(refreshToken);
        
        String tokenType = decodedJWT.getClaim(CLAIM_TOKEN_TYPE).asString();
        if (!TOKEN_TYPE_REFRESH.equals(tokenType)) {
            throw new JWTVerificationException("无效的RefreshToken");
        }

        Long userId = decodedJWT.getClaim(CLAIM_USER_ID).asLong();
        String username = decodedJWT.getClaim(CLAIM_USERNAME).asString();

        String newAccessToken = generateAccessToken(userId, username);
        String newRefreshToken = generateRefreshToken(userId, username);

        // 将旧refreshToken加入黑名单
        blacklistToken(refreshToken);

        return Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "expiresIn", accessTokenExpire,
                "tokenType", "Bearer"
        );
    }
}
