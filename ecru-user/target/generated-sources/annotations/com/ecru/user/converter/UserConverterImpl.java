package com.ecru.user.converter;

import com.ecru.user.dto.RegisterDTO;
import com.ecru.user.entity.User;
import com.ecru.user.vo.RegisterVO;
import com.ecru.user.vo.UserVO;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-04T16:07:13+0800",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.9 (Oracle Corporation)"
)
public class UserConverterImpl implements UserConverter {

    @Override
    public User toEntity(RegisterDTO dto) {
        if ( dto == null ) {
            return null;
        }

        User user = new User();

        user.setUsername( dto.getUsername() );
        user.setEmail( dto.getEmail() );
        user.setPhone( dto.getPhone() );

        return user;
    }

    @Override
    public RegisterVO toRegisterVO(User user) {
        if ( user == null ) {
            return null;
        }

        RegisterVO.RegisterVOBuilder registerVO = RegisterVO.builder();

        registerVO.userId( user.getId() );
        registerVO.username( user.getUsername() );
        registerVO.email( user.getEmail() );
        registerVO.createdAt( user.getCreatedAt() );

        return registerVO.build();
    }

    @Override
    public UserVO toUserVO(User user) {
        if ( user == null ) {
            return null;
        }

        UserVO.UserVOBuilder userVO = UserVO.builder();

        userVO.userId( user.getId() );
        userVO.username( user.getUsername() );
        userVO.email( user.getEmail() );
        userVO.phone( user.getPhone() );
        userVO.avatarUrl( user.getAvatarUrl() );
        userVO.nickname( user.getNickname() );
        userVO.gender( user.getGender() );
        userVO.birthday( user.getBirthday() );
        userVO.status( user.getStatus() );
        userVO.lastLoginAt( user.getLastLoginAt() );
        userVO.createdAt( user.getCreatedAt() );

        return userVO.build();
    }
}
