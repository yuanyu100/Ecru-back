package com.ecru.web.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * 管理 MySQL 和 PostgreSQL 两个数据源
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL 数据源（主数据源）
     */
    @Primary
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource mysqlDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * PostgreSQL 数据源（向量存储）
     */
    @Bean(name = "pgDataSource")
    @ConfigurationProperties(prefix = "spring.datasource-pg")
    public DataSource pgDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * MySQL SqlSessionFactory
     */
    @Primary
    @Bean(name = "mysqlSqlSessionFactory")
    public SqlSessionFactory mysqlSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        
        // 暂时不设置Mapper路径，使用默认配置
        // bean.setMapperLocations(new PathMatchingResourcePatternResolver()
        //         .getResources("classpath*:com/ecru/user/mapper/*.xml"));
        
        return bean.getObject();
    }

    /**
     * PostgreSQL SqlSessionFactory
     */
    @Bean(name = "pgSqlSessionFactory")
    public SqlSessionFactory pgSqlSessionFactory(@Qualifier("pgDataSource") DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        // 设置Mapper路径（PostgreSQL专用）
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/postgres/**/*.xml"));
        return bean.getObject();
    }
}
