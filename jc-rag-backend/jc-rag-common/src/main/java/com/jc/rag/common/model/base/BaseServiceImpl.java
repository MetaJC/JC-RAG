package com.jc.rag.common.model.base;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 通用 ServiceImpl 基类。
 * <p>约束 M 必须继承我们的 BaseMapper，T 为实体类型。
 * <p>abstract：禁止直接实例化，必须由具体业务的 XxxServiceImpl 继承。
 *
 * @author JC
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements BaseService<T> {
}
