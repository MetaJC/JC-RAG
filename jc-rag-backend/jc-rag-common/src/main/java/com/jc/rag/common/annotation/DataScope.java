/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解（占位）
 * <p>标注在 Service 方法的入参 VO/Param 字段上，AOP 拦截后注入当前用户的 dataScope 过滤条件。
 * <p>本注解仅作占位定义，AOP 切面实现待数据权限需求时再补，避免过度设计。
 * <p>字段级标注示例：
 * <pre>{@code
 * public class UserQueryParam {
 *     @DataScope(scope = DataScope.SELF, userField = "created_by")
 *     private Long ownerId;
 * }
 * }</pre>
 * @author JC
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /** 数据权限范围（默认 ALL = 不注入过滤） */
    com.jc.rag.common.enums.DataScope scope() default com.jc.rag.common.enums.DataScope.ALL;

    /** 实体中标识"创建人"的字段名（默认 createdBy） */
    String userField() default "createdBy";

    /** 实体中标识"部门"的字段名（默认 deptId） */
    String deptField() default "deptId";
}
