package com.jc.rag.common.web.page;

/**
 * 通用分页请求 DTO (内部 DAO 透传使用)。
 * <p>仅承载分页字段 (pageNum/pageSize); 业务过滤字段 (keyword/status 等) 由业务 Param 承载, 不混入此类。
 * <p>Controller 调用 Service 业务查询方法时, 直接传入业务 Param (如 RagSkillQueryParam); 仅在 Service 内部需要与 MyBatis 分页插件 / DAO 交互时, 通过 {@link #of(int, int)} 或 {@link PageQueryParam#toPageRequest()} 构造。
 *
 * @author JC
 */
public class PageRequest {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 200;

    private int pageNum = DEFAULT_PAGE;
    private int pageSize = DEFAULT_SIZE;

    public PageRequest() {}

    public PageRequest(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    /** 工厂方法, 边界安全。 */
    public static PageRequest of(int pageNum, int pageSize) {
        return new PageRequest(Math.max(1, pageNum), Math.max(1, Math.min(MAX_SIZE, pageSize)));
    }

    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = Math.max(1, pageNum); }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = Math.max(1, Math.min(MAX_SIZE, pageSize)); }

    public int getOffset() { return (pageNum - 1) * pageSize; }
}
