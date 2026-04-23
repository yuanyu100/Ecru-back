package com.ecru.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请先登录"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 用户相关 1000-1999
    USER_NOT_FOUND(1001, "用户不存在"),
    USERNAME_EXISTS(1002, "用户名已存在"),
    EMAIL_EXISTS(1003, "邮箱已存在"),
    PHONE_EXISTS(1004, "手机号已存在"),
    INVALID_CREDENTIALS(1005, "用户名或密码错误"),
    USER_DISABLED(1006, "用户已被禁用"),
    OLD_PASSWORD_ERROR(1007, "旧密码错误"),

    // 衣物相关 2000-2999
    CLOTHING_NOT_FOUND(2001, "衣物不存在"),
    CLOTHING_NOT_BELONG_TO_USER(2002, "衣物不属于当前用户"),

    // 文件上传 3000-3999
    FILE_UPLOAD_ERROR(3001, "文件上传失败"),
    FILE_TYPE_NOT_ALLOWED(3002, "不支持的文件类型"),
    FILE_SIZE_EXCEEDED(3003, "文件大小超出限制"),

    // Token相关 4000-4999
    TOKEN_EXPIRED(4001, "Token已过期"),
    TOKEN_INVALID(4002, "Token无效"),
    REFRESH_TOKEN_EXPIRED(4003, "Refresh Token已过期");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
