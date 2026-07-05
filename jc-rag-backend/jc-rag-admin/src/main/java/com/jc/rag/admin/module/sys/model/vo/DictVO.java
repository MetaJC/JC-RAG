package com.jc.rag.admin.module.sys.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典项 VO
 * @author JC
 */
@Data
public class DictVO {
    private Long id;
    private String typeCode;
    private String itemCode;
    private String itemName;
    private Integer sort;
    private Integer status;
    private LocalDateTime createdAt;
}
