package com.jc.rag.common.web.page;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 统一分页响应。
 * <p>替换 MyBatis-Plus 原生 Page / IPage, 字段名固定 records/total/page/size, 前端按此约定解析。
 * <p>序列化字段顺序: records / total / page / size, 与前端分页组件直接对齐。
 *
 * @author JC
 */
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页数据 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码 (从 1 开始) */
    private int page;
    /** 每页大小 */
    private int size;

    public PageResponse() {}

    public PageResponse(List<T> records, long total, int page, int size) {
        this.records = records == null ? Collections.emptyList() : records;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /** 当前页数据 */
    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }
    /** 总记录数 */
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    /** 当前页码 (从 1 开始) */
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    /** 每页大小 */
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    /** 工厂: 列表 + 统计字段 */
    public static <T> PageResponse<T> of(List<T> records, long total, int page, int size) {
        return new PageResponse<>(records, total, page, size);
    }

    /** 工厂: MyBatis-Plus IPage -> PageResponse */
    public static <T> PageResponse<T> of(IPage<T> page) {
        return new PageResponse<>(page.getRecords(), page.getTotal(), (int) page.getCurrent(), (int) page.getSize());
    }

    /** 类型转换 (e.g. DO -> VO) */
    public <R> PageResponse<R> map(Function<? super T, ? extends R> converter) {
        List<R> mapped = this.records == null ? Collections.emptyList()
                : this.records.stream().map(converter).collect(Collectors.toList());
        return new PageResponse<>(mapped, this.total, this.page, this.size);
    }

    /** 空分页 (总数 0) */
    public static <T> PageResponse<T> empty(int page, int size) {
        return new PageResponse<>(Collections.emptyList(), 0L, page, size);
    }
}
