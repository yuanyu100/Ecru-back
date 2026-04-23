package com.ecru.clothing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.clothing.entity.ClothingStyleTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

@Mapper
public interface ClothingStyleTagMapper extends BaseMapper<ClothingStyleTag> {

    @Select("SELECT * FROM clothing_style_tag WHERE clothing_id = #{clothingId}")
    List<ClothingStyleTag> selectByClothingId(@Param("clothingId") Long clothingId);

    @Delete("DELETE FROM clothing_style_tag WHERE clothing_id = #{clothingId}")
    void deleteByClothingId(@Param("clothingId") Long clothingId);

}
