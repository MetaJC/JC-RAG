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


/**
 * JC RAG Pro 商业版路由扩展点.
 * <p>★ 商业版构建脚本 (build-pro.sh) 会覆盖此文件 ★</p>
 * <p>开源版构建时, 此文件导出空数组, 不影响任何路由.</p>
 * <p>商业版构建时, 此文件被 jc-rag-pro-core/jc-rag-pro-frontend/src/pro-router.ts 覆盖,
 * 里面的同 path 路由会覆盖开源版路由, 新增路由会注入到 Pro 扩展.</p>
 *
 * @author JC
 */
import type { RouteRecordRaw } from 'vue-router'

// 开源版默认: 空数组 (无商业扩展)
export const proExtensionRoutes: RouteRecordRaw[] = []
