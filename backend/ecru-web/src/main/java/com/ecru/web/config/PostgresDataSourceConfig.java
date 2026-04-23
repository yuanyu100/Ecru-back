package com.ecru.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PostgreSQL 数据源配置
 */
@Slf4j
@Configuration
public class PostgresDataSourceConfig {

    /**
     * PostgreSQL 数据源
     */
    @Bean(name = "postgresDataSource")
    public DataSource postgresDataSource() {
        log.info("创建 PostgreSQL 数据源");
        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5432/postgres")
                .username("postgres")
                .password("040726fan")
                .driverClassName("org.postgresql.Driver")
                .build();
        log.info("PostgreSQL 数据源创建成功");
        return dataSource;
    }

    /**
     * PostgreSQL JdbcTemplate
     */
    @Bean(name = "postgresJdbcTemplate")
    public JdbcTemplate postgresJdbcTemplate(@Qualifier("postgresDataSource") DataSource dataSource) {
        log.info("创建 PostgreSQL JdbcTemplate");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("PostgreSQL JdbcTemplate 创建成功");
        return jdbcTemplate;
    }
}
