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

}
