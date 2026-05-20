package com.ecru.web.vo;

import com.ecru.outfit.entity.UserStyleArchive;
import com.ecru.user.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "管理员用户详情视图")
public class AdminUserDetailVO {

    @Schema(description = "用户基础资料")
    private UserVO user;

    @Schema(description = "风格档案")
    private UserStyleArchive styleProfile;

    @Schema(description = "衣物数量")
    private Long clothingCount;
}
