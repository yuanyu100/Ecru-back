package com.ecru.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_login_logs")
public class UserLoginLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer loginType;

    private String loginIp;

    private String loginDevice;

    private String loginLocation;

    private Integer loginStatus;

    private String failReason;

    private LocalDateTime createdAt;
}
