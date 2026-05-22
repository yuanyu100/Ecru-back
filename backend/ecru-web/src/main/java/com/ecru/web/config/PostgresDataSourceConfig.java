package com.ecru.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class PostgresDataSourceConfig {

    @Bean(name = "postgresJdbcTemplate")
    public JdbcTemplate postgresJdbcTemplate(@Qualifier("pgDataSource") DataSource dataSource) {
        log.info("Creating PostgreSQL JdbcTemplate");
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        log.info("PostgreSQL JdbcTemplate created");
        return jdbcTemplate;
    }
}
