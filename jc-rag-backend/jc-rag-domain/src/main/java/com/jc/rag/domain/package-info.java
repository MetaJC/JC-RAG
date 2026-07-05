/**
 * 共享领域层。
 * <p>
 * 为 jc-rag-admin（管理端）和 jc-rag-user（用户端）提供：
 * <ul>
 *   <li>实体类（entity.{rbac|sys|chat|rag}）</li>
 *   <li>Mapper 接口（mapper.{rbac|sys|chat|rag}）</li>
 *   <li>MyBatis XML 映射文件（resources/mapper/{rbac|sys|chat|rag}）</li>
 *   <li>通用常量（constants）、枚举（enums）</li>
 * </ul>
 * <p>本模块不包含业务 Service、Controller，保证领域层纯净。
 *
 * @author JC
 */
package com.jc.rag.domain;
