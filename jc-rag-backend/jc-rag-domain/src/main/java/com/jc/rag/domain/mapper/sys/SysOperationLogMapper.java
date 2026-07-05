package com.jc.rag.domain.mapper.sys;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统操作日志 Mapper
 * @author JC
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 条件计数
     */
    long countOperationLogs(@Param("moduleName") String moduleName,
                            @Param("operationType") String operationType,
                            @Param("username") String username,
                            @Param("status") String status,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /**
     * 条件分页查询
     */
    IPage<SysOperationLog> pageOperationLogs(IPage<SysOperationLog> page,
                                             @Param("moduleName") String moduleName,
                                             @Param("operationType") String operationType,
                                             @Param("username") String username,
                                             @Param("status") String status,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 全量导出 (无分页)
     */
    List<SysOperationLog> listOperationLogs(@Param("moduleName") String moduleName,
                                             @Param("operationType") String operationType,
                                             @Param("username") String username,
                                             @Param("status") String status,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);
}
