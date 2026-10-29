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


package com.jc.rag.admin.module.rbac.position.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 岗位新增 / 修改 Param。
 * <p>岗位与部门解耦: 不再传 departmentId, 用户的部门归属走 rbac_user_department。
 *
 * @author JC
 */
public class RbacPositionSaveParam {

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 50, message = "岗位名称长度不能超过 50")
    private String name;

    /**
     * 显示名 (简称 / 英文名, 例如: 岗位名=人力资源, 显示名=HR)
     */
    @Size(max = 50, message = "显示名长度不能超过 50")
    private String displayName;

    /**
     * 岗位级别: 1 初级 / 2 中级 / 3 高级
     */
    @NotNull(message = "岗位级别不能为空")
    private Integer level;

    private Integer sort;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;

    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String v) { displayName = v; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer v) { level = v; }
    public Integer getSort() { return sort; }
    public void setSort(Integer v) { sort = v; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer v) { status = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { remark = v; }
}
