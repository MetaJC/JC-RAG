package com.jc.rag.admin.module.sys.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统设置保存参数 (系统设置页提交).
 *
 * @author JC
 */
@Data
public class SystemSettingParam {

    /** 系统名称 */
    @NotBlank(message = "系统名称不能为空")
    @Size(max = 64, message = "系统名称不能超过 64 个字符")
    private String systemName;

    /** 系统 Logo URL (可为空, 空表示使用默认) */
    @Size(max = 512, message = "Logo 地址不能超过 512 个字符")
    private String logoUrl;

    /** 版权信息 */
    @Size(max = 256, message = "版权信息不能超过 256 个字符")
    private String copyright;

    /** 是否开放用户注册 */
    @NotNull(message = "注册开关不能为空")
    private Boolean registerOpen;

    /** 登录是否必须勾选用户协议 */
    @NotNull(message = "协议勾选设置不能为空")
    private Boolean loginAgreeRequired;
}
