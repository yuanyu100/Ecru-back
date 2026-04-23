package com.ecru.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "更新用户信息请求")
public class UpdateUserDTO {

    @Schema(description = "昵称", example = "张三")
    @Size(max = 50, message = "昵称长度不能超过50位")
    private String nickname;

    @Schema(description = "性别：0-未知，1-男，2-女", example = "1")
    @Min(value = 0, message = "性别格式不正确")
    @Max(value = 2, message = "性别格式不正确")
    private Integer gender;

    @Schema(description = "生日", example = "1990-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}
