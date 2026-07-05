package com.jc.rag.admin.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解 - AOP 自动记录
 * 贴在 Controller 方法上, 切面会自动捕获并持久化
 *
 * @author JC
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块名 (如 RBAC/SYSTEM) */
    String module() default "";

    /** 操作类型 (如 CREATE_USER) */
    String action() default "";

    /** 操作描述 */
    String description() default "";

    /** 是否记录请求参数 (默认 true) */
    boolean recordArgs() default true;

    /** 是否记录响应结果 (默认 true, 含大数据可关闭) */
    boolean recordResult() default true;
}
