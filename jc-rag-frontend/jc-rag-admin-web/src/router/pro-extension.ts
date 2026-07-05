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
