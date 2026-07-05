package com.jc.rag.domain.entity.knowledge;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 知识库-角色授权 实体。
 * <p>对应 kb_role_auth 表, 记录某知识库对某角色授予的操作权限。
 * <p>perm_level: 1 VIEW / 2 EDIT / 3 MANAGE。
 *
 * @author JC
 */
@TableName("kb_role_auth")
public class KbRoleAuthEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 知识库 ID */
    @TableField("kb_id")
    private Long kbId;

    /** 角色 ID */
    @TableField("role_id")
    private Long roleId;

    /** 权限级别 1/2/3 */
    @TableField("perm_level")
    private Integer permLevel;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getKbId() { return kbId; }
    public void setKbId(Long kbId) { this.kbId = kbId; }

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public Integer getPermLevel() { return permLevel; }
    public void setPermLevel(Integer permLevel) { this.permLevel = permLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
