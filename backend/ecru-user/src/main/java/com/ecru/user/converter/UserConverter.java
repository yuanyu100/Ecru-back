package com.ecru.user.converter;

import com.ecru.user.dto.RegisterDTO;
import com.ecru.user.entity.User;
import com.ecru.user.vo.RegisterVO;
import com.ecru.user.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConverter {

    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    /**
     * RegisterDTO -> User
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "birthday", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "lastLoginIp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterDTO dto);

    /**
     * User -> RegisterVO
     */
    @Mapping(source = "id", target = "userId")
    RegisterVO toRegisterVO(User user);

    /**
     * User -> UserVO
     */
    @Mapping(source = "id", target = "userId")
    UserVO toUserVO(User user);
}
