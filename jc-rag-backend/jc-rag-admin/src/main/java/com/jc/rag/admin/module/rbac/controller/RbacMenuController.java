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


package com.jc.rag.admin.module.rbac.controller;

import com.jc.rag.admin.module.rbac.model.param.RbacMenuMoveParam;
import com.jc.rag.admin.module.rbac.model.param.RbacMenuSaveParam;
import com.jc.rag.admin.module.rbac.model.vo.RbacMenuVO;
import com.jc.rag.admin.module.rbac.service.RbacMenuService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理 Controller (基于权限 permType=MENU 子集)
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/rbac/menu")
@RequiredArgsConstructor
public class RbacMenuController {

    private final RbacMenuService rbacMenuService;

    @GetMapping("/tree")
    public R<List<RbacMenuVO>> tree() {
        return R.ok(rbacMenuService.listMenuTree());
    }

    @GetMapping("/user-tree")
    public R<List<RbacMenuVO>> userTree() {
        return R.ok(rbacMenuService.listMenuTreeByUserId(currentUserId()));
    }

    /**
     * 菜单扁平列表（按 permType=MENU 过滤），
     * 供 RbacManageView「菜单管理」tab 渲染使用。
     */
    @GetMapping
    public R<PageResponse<RbacMenuVO>> list() {
        List<RbacMenuVO> records = rbacMenuService.listMenus();
        return R.ok(PageResponse.of(records, records.size(), 1, Math.max(records.size(), 1)));
    }

    @PostMapping
    public R<Long> create(@RequestBody @Valid RbacMenuSaveParam param) {
        return R.ok(rbacMenuService.createMenu(param));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody @Valid RbacMenuSaveParam param) {
        return R.ok(rbacMenuService.updateMenu(id, param));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(rbacMenuService.deleteMenu(id));
    }

    /**
     * 拖拽移动节点 (前端 el-tree @node-drop 调用)
     * <p>Body: { parentId?: number, sort?: number }
     * <ul>
     *   <li>parentId=0 / null 表示移到根</li>
     *   <li>sort 由前端按 drop position (before/after/inner) 计算后传入</li>
     * </ul>
     */
    @PutMapping("/{id}/move")
    public R<Boolean> move(@PathVariable Long id, @RequestBody RbacMenuMoveParam param) {
        return R.ok(rbacMenuService.moveMenu(id, param.getParentId(), param.getSort()));
    }

    private Long currentUserId() {
        return rbacMenuService.currentUserId();
    }
}
