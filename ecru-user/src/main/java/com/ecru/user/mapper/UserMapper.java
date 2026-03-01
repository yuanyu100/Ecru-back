package com.ecru.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecru.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询用户
     */
    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User selectByPhone(@Param("phone") String phone);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 检查手机号是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user WHERE phone = #{phone}")
    boolean existsByPhone(@Param("phone") String phone);
}
