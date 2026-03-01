package com.ecru.outfit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 数据库初始化器
 * 用于启用pgvector扩展
 */
@Slf4j
@Component
public class DatabaseInitializer {

    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * 初始化数据库
     */
    @PostConstruct
    public void init() {
        try {
            // 启用pgvector扩展
            enablePgVectorExtension();
            log.info("数据库初始化成功");
        } catch (Exception e) {
            log.error("数据库初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 启用pgvector扩展
     */
    private void enablePgVectorExtension() {
        try {
            // 检查pgvector扩展是否已安装
            String checkSql = "SELECT 1 FROM pg_extension WHERE extname = 'vector'";
            Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (exists == null) {
                // 安装pgvector扩展
                String installSql = "CREATE EXTENSION IF NOT EXISTS vector";
                jdbcTemplate.execute(installSql);
                log.info("pgvector 扩展已安装");
            } else {
                log.info("pgvector 扩展已存在");
            }
        } catch (Exception e) {
            log.error("启用pgvector扩展失败: {}", e.getMessage());
            // 继续执行，可能是因为数据库用户没有权限安装扩展
            // 在生产环境中，应该确保数据库管理员已经安装了pgvector扩展
        }
    }

}