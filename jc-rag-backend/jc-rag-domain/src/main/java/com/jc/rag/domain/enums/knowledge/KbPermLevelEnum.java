package com.jc.rag.domain.enums.knowledge;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 知识库操作权限级别。
 * <p>三级: VIEW (查看) / EDIT (编辑) / MANAGE (管理)。
 * <p>包含关系: MANAGE ⊇ EDIT ⊇ VIEW (高权限自动覆盖低权限)。
 *
 * @author JC
 */
public enum KbPermLevelEnum {

    NONE(0, "无"),
    VIEW(1, "查看"),
    EDIT(2, "编辑"),
    MANAGE(3, "管理");

    @EnumValue
    @JsonValue
    private final int code;
    private final String label;

    KbPermLevelEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() { return code; }
    public String getLabel() { return label; }
    public String getText() { return label; }

    public static KbPermLevelEnum of(Integer code) {
        if (code == null) return null;
        for (KbPermLevelEnum e : values()) {
            if (e.code == code) return e;
        }
        return null;
    }

    /**
     * 兼容数据库中可能为 null 的场景, 不可识别的 code 一律按 NONE 处理.
     */
    public static KbPermLevelEnum ofCode(Integer code) {
        KbPermLevelEnum e = of(code);
        return e == null ? NONE : e;
    }

    /**
     * 当前权限是否覆盖目标权限. MANAGE 覆盖 EDIT/VIEW, EDIT 覆盖 VIEW.
     */
    public boolean covers(KbPermLevelEnum need) {
        if (need == null) return true;
        return this.code >= need.code;
    }

    /**
     * 取两权限中较高者.
     */
    public static KbPermLevelEnum max(KbPermLevelEnum a, KbPermLevelEnum b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.code >= b.code ? a : b;
    }
}
