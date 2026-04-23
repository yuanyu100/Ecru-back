package com.ecru.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UserContext {

    private static final String USER_ID_KEY = "userId";
    private static final String USERNAME_KEY = "username";
    private static final String TOKEN_TYPE_KEY = "tokenType";
    private static final String TOKEN_KEY = "token";

    public static Long getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return (Long) request.getAttribute(USER_ID_KEY);
    }

    public static String getCurrentUsername() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return (String) request.getAttribute(USERNAME_KEY);
    }

    public static String getCurrentTokenType() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return (String) request.getAttribute(TOKEN_TYPE_KEY);
    }

    public static String getCurrentToken() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }
        return (String) request.getAttribute(TOKEN_KEY);
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isAuthenticated() {
        return getCurrentUserId() != null;
    }

}
