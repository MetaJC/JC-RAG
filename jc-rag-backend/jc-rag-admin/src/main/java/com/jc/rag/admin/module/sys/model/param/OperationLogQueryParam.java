package com.jc.rag.admin.module.sys.model.param;

import com.jc.rag.common.web.page.PageQueryParam;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询 Param。
 * <p>分页字段 {@code page/size} 由父类 {@link PageQueryParam} 提供。
 * @author JC
 */
@Data
public class OperationLogQueryParam extends PageQueryParam {

    private String moduleName;

    private String operationType;

    private String username;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
