package com.jc.rag.user.module.auth.model.vo;

import java.io.Serializable;

/**
 * 滑动验证码需求状态 VO。
 * <p>前端刷新页面后调用 captcha-status 接口恢复"是否需要滑动验证"状态:
 * 失败次数达到阈值即保持出现, 仅登录成功后由后端清零。
 *
 * @author JC
 */
public class CaptchaStatusVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 是否必须先完成滑动验证才能登录 */
    private Boolean needCaptcha;

    /** 当前连续登录失败次数 */
    private Integer failCount;

    public static CaptchaStatusVO of(boolean needCaptcha, int failCount) {
        CaptchaStatusVO vo = new CaptchaStatusVO();
        vo.setNeedCaptcha(needCaptcha);
        vo.setFailCount(failCount);
        return vo;
    }

    public Boolean getNeedCaptcha() { return needCaptcha; }
    public void setNeedCaptcha(Boolean needCaptcha) { this.needCaptcha = needCaptcha; }
    public Integer getFailCount() { return failCount; }
    public void setFailCount(Integer failCount) { this.failCount = failCount; }
}
