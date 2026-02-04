package com.ecru.web.interceptor;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.ecru.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = extractToken(request);
        if (token != null) {
            try {
                DecodedJWT decodedJWT = jwtUtil.verifyToken(token);
                Long userId = decodedJWT.getClaim("userId").asLong();
                String username = decodedJWT.getClaim("username").asString();
                String tokenType = decodedJWT.getClaim("tokenType").asString();
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("tokenType", tokenType);
                request.setAttribute("token", token);
            } catch (Exception e) {
                log.debug("Token验证失败: {}", e.getMessage());
            }
        }
        return true;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
