package com.jc.rag.common.web.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 分页工具.
 * <p>Service 层分页查询唯一推荐入口: pageOf() 构建 MP Page, Mapper XML 走分页拦截器, toPageResponse() 包装响应.
 * <p>严禁手算 offset / 手写 LIMIT 分页 SQL.
 * <p>keyword 模糊匹配: buildLikeClause() 统一处理转义.
 * @author JC
 */
public class PageHelper {

    private PageHelper() {}

    /**
     * 泛型版: IPage&lt;T&gt; -> PageResponse&lt;V&gt;.
     * <p>Service 层分页查询的唯一推荐入口, 禁止手算 offset.
     * <p>适用: T 为任意类型 (Entity / Map / DTO). 动态列 SQL 也可传 IPage&lt;Map&gt; + Function&lt;Map, V&gt;.
     *
     * @param page   MP 分页对象 (records/total/current/size 由拦截器填充)
     * @param mapper 记录 -> VO 转换函数
     */
    @SuppressWarnings("unchecked")
    public static <T, V> PageResponse<V> toPageResponse(IPage<T> page, Function<T, V> mapper) {
        int pageNum = (int) Math.max(1L, page.getCurrent());
        int pageSize = (int) Math.max(1L, page.getSize());
        List<V> records = Optional.ofNullable(page.getRecords()).orElse(List.of())
                .stream().map(mapper).toList();
        return PageResponse.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 从分页参数构建 MP Page 对象 (不查总数, 由业务自行设置).
     * <p>统一封装: 避免每个 Service 重复写 new Page&lt;&gt;(pageNum, pageSize, false).
     */
    public static <T> Page<T> pageOf(PageQueryParam q, Class<T> entityClass) {
        int pageNum = q == null ? 1 : q.getPageNum();
        int pageSize = q == null ? 10 : q.getPageSize();
        return new Page<>(pageNum, pageSize, false);
    }

    /** 类型擦除场景: 无 entityClass, 调用方自行强转. */
    @SuppressWarnings("unchecked")
    public static <T> Page<T> pageOf(PageQueryParam q) {
        return (Page<T>) pageOf(q, Object.class);
    }

    /**
     * 构建 LIKE 子句: 自动转义 % 和 _, 空白 keyword 返回 null.
     * @param keyword 原始关键字
     * @return 包装后的 LIKE 值 (含通配符), 或 null
     */
    public static String buildLikeClause(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
