package com.ecru.web;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.ecru"})
@MapperScan(
    basePackages = {
        "com.ecru.common.mapper",
        "com.ecru.user.mapper",
        "com.ecru.clothing.mapper",
        "com.ecru.outfit.mapper"
    },
    annotationClass = Mapper.class
)
public class EcruApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcruApplication.class, args);
    }

}
