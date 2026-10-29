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
 * 前端版本标识 (开源版 / 商业版).
 * <p>
 * 构建期由 {@code VITE_EDITION} 决定 (默认 oss), 运行时由后端 {@code /api/admin/edition} 二次校验.
 * 后端为准, 前端仅做 UI 隐藏 / 提示.
 *
 * @author JC
 */

export type Edition = 'oss' | 'pro'

/** 构建期版本 (来自 import.meta.env.VITE_EDITION, 默认 oss). */
export const BUILD_EDITION: Edition =
  (import.meta.env.VITE_EDITION as Edition) || 'oss'

/** 构建期版本中文标签. */
export const BUILD_EDITION_LABEL: string =
  import.meta.env.VITE_EDITION_LABEL || 'Community Edition'

/** 是否为商业版构建. */
export const IS_PRO_BUILD: boolean = BUILD_EDITION === 'pro'
