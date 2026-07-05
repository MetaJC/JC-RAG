package com.jc.rag.common.model.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jc.rag.common.exception.ResourceNotFoundException;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;

import java.io.Serializable;

/**
 * 通用 Service 基接口。
 * <p>在 MP IService 之上补充业务常用方法（强校验、统一分页）。
 *
 * @author JC
 */
public interface BaseService<T> extends IService<T> {

    /** 按 id 查询，找不到抛 {@link ResourceNotFoundException} (i18n key: common.id.notfound)。 */
    default T getByIdRequired(Serializable id) {
        T t = getById(id);
        if (t == null) {
            throw new ResourceNotFoundException("common.id.notfound", id);
        }
        return t;
    }

    /** 统一分页入口，返回 PageResponse 与前端约定。 */
    default PageResponse<T> pageList(Wrapper<T> wrapper, PageRequest pr) {
        IPage<T> p = page(new Page<>(pr.getPageNum(), pr.getPageSize()), wrapper);
        return new PageResponse<>(p.getRecords(), p.getTotal(), pr.getPageNum(), pr.getPageSize());
    }
}
