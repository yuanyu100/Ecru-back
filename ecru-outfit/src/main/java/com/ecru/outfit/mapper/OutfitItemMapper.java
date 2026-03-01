package com.ecru.outfit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.outfit.entity.OutfitItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 搭配单品关联Mapper
 */
@Mapper
public interface OutfitItemMapper extends BaseMapper<OutfitItem> {

    /**
     * 根据搭配建议ID查询单品列表
     * @param outfitAdviceId 搭配建议ID
     * @return 单品列表
     */
    List<OutfitItem> selectByOutfitAdviceId(@Param("outfitAdviceId") Long outfitAdviceId);

    /**
     * 根据搭配建议ID删除所有关联单品
     * @param outfitAdviceId 搭配建议ID
     * @return 删除数量
     */
    Integer deleteByOutfitAdviceId(@Param("outfitAdviceId") Long outfitAdviceId);

    /**
     * 批量插入搭配单品
     * @param items 单品列表
     * @return 插入数量
     */
    Integer batchInsert(@Param("items") List<OutfitItem> items);

}
