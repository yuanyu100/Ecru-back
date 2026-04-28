package com.ecru.user.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private Integer loginType;

    private String loginIp;

    private String loginDevice;

    private String loginLocation;

    private Integer loginStatus;

    private String failReason;

    private LocalDateTime createdAt;
}
