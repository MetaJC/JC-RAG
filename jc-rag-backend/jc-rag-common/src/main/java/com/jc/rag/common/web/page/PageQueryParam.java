package com.jc.rag.common.web.page;

/**
 * 通用分页查询参数基类。
 * <p>统一封装 pageNum/pageSize 默认值与上下界, 子类只需继承即可获得标准分页字段, 无需重复声明。
 * <p>Controller 推荐用法:
 * <pre>
 *  {@code
 *  @GetMapping
 *  public R<PageResponse<XxxVO>> list(XxxQueryParam query) {
 *      return R.ok(service.page(query));
 *  }
 *  }
 * </pre>
 * <p>默认 size=10, 上限 200; 边界校验通过 setter 强制, 不依赖 jakarta.validation, 保持 common 模块零依赖。
 * <p>若业务需在 Controller 入口触发 {@code @Valid} 校验, 可在子类上手动加 {@code @Min}/{@code @Max} 注解 (要求 Controller 模块引入 validation-api)。
 * <p>字段命名 {@code pageNum}/{@code pageSize} 与前端既有 query 参数保持一致, 避免破坏性变更。
 *
 * @author JC
 */
public class PageQueryParam {

    /** 默认页码 */
    public static final int DEFAULT_PAGE = 1;
    /** 默认每页大小 */
    public static final int DEFAULT_SIZE = 10;
    /** 单页最大条数 (防止误传大值拖垮服务) */
    public static final int MAX_SIZE = 200;

    /** 当前页码 (从 1 开始) */
    private int pageNum = DEFAULT_PAGE;

    /** 每页大小 */
    private int pageSize = DEFAULT_SIZE;

    public int getPageNum() { return pageNum; }

    public void setPageNum(int pageNum) {
        this.pageNum = Math.max(1, pageNum);
    }

    public int getPageSize() { return pageSize; }

    public void setPageSize(int pageSize) {
        this.pageSize = Math.max(1, Math.min(MAX_SIZE, pageSize));
    }

    /** 转换为 {@link PageRequest}, 传递给 Service 层使用 */
    public PageRequest toPageRequest() {
        return new PageRequest(pageNum, pageSize);
    }
}
