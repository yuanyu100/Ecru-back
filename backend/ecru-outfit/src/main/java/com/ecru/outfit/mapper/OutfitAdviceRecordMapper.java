package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.OutfitAdviceRecord;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 搭配建议记录Mapper
 */
@Mapper
public interface OutfitAdviceRecordMapper extends BaseMapper<OutfitAdviceRecord> {

    /**
     * 根据用户ID查询历史搭配记录
     * @param userId 用户ID
     * @return 搭配记录列表
     */
    Page<OutfitAdviceRecord> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和场合查询搭配记录
     * @param userId 用户ID
     * @param occasion 场合
     * @param limit 限制数量
     * @return 搭配记录列表
     */
    List<OutfitAdviceRecord> selectByUserIdAndOccasion(@Param("userId") Long userId, @Param("occasion") String occasion, @Param("limit") Integer limit);

    /**
     * 查询用户收藏的搭配记录
     * @param userId 用户ID
     * @return 搭配记录列表
     */
    Page<OutfitAdviceRecord> selectFavoritesByUserId(@Param("userId") Long userId);

    /**
     * 统计用户搭配记录数量
     * @param userId 用户ID
     * @return 记录数量
     */
    Integer countByUserId(@Param("userId") Long userId);

    /**
     * 查询用户当前保存的首页推荐记录
     * 约定使用 input_type=3 + occasion=HOME_DAILY 作为首页推荐标识
     */
    List<OutfitAdviceRecord> selectHomeRecommendations(@Param("userId") Long userId, @Param("limit") Integer limit);

    /**
     * 删除用户旧的首页推荐记录
     * 首页推荐可重建，因此刷新时采用“删旧写新”的策略
     */
    Integer deleteHomeRecommendations(@Param("userId") Long userId);

}
