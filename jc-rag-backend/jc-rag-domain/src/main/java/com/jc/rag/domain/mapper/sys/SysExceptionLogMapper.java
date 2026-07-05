package com.jc.rag.domain.mapper.sys;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jc.rag.domain.entity.sys.SysExceptionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统异常日志 Mapper
 * @author JC
 */
@Mapper
public interface SysExceptionLogMapper extends BaseMapper<SysExceptionLog> {

    /**
     * 条件分页查询 (时间格式化, stackTrace 截断)
     */
    IPage<Map<String, Object>> pageExceptionLogs(IPage<Map<String, Object>> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("moduleName") String moduleName);

    /** 全量导出 */
    List<Map<String, Object>> listExceptionLogs(
            @Param("keyword") String keyword,
            @Param("moduleName") String moduleName
    );

    /**
     * 条件计数
     */
    long countExceptionLogs(
            @Param("keyword") String keyword,
            @Param("moduleName") String moduleName
    );
}
