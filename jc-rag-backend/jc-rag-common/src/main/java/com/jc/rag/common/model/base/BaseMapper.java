package com.jc.rag.common.model.base;

/**
 * 通用 Mapper 基接口。
 * <p>所有自定义 Mapper 必须继承本接口，禁止直接继承 MP BaseMapper，便于未来扩展通用方法。
 *
 * @author JC
 */
public interface BaseMapper<T> extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T> {
}
