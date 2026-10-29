/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.domain.mapper.sys;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jc.rag.domain.entity.sys.SecurityAuditLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 安全审计日志 Mapper。
 *
 * @author JC
 */
@Mapper
public interface SecurityAuditLogMapper extends BaseMapper<SecurityAuditLogEntity> {

    /**
     * 分页查询审计日志, 支持按 auditType / riskLevel / userId / 时间范围 过滤.
     * <p>用 ${ew.customSqlSegment} 让 MyBatis-Plus 自动拼接 QueryWrapper, 避免 XML 维护.
     */
    default IPage<SecurityAuditLogEntity> pageAudit(Page<SecurityAuditLogEntity> page,
                                                     QueryWrapper<SecurityAuditLogEntity> qw) {
        return selectPage(page, qw);
    }

    /**
     * 统计指定时间窗口内 DENY/MASK 命中数 (用于首页看板).
     */
    @Select("SELECT COUNT(*) FROM security_audit_log WHERE action = #{action} AND created_at >= #{since}")
    long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);
}

