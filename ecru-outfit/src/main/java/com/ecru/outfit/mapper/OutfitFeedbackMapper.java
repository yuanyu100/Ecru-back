package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.OutfitFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 搭配反馈Mapper
 */
@Mapper
public interface OutfitFeedbackMapper extends BaseMapper<OutfitFeedback> {

    /**
     * 根据搭配建议ID和用户ID查询反馈
     * @param outfitAdviceId 搭配建议ID
     * @param userId 用户ID
     * @return 反馈记录
     */
    OutfitFeedback selectByOutfitAndUser(@Param("outfitAdviceId") Long outfitAdviceId, @Param("userId") Long userId);

    /**
     * 根据搭配建议ID查询反馈
     * @param outfitAdviceId 搭配建议ID
     * @return 反馈记录
     */
    OutfitFeedback selectByOutfitAdviceId(@Param("outfitAdviceId") Long outfitAdviceId);

}
