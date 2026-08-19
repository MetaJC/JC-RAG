package com.jc.rag.user.module.auth.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 短信验证码发送结果 VO。
 * <p>sent=true 表示本次成功发送; sent=false 时 waitSeconds 为距下次可发送的剩余秒数
 * (前端据此同步倒计时, 保证与后端限流一致)。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsSendResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean sent;
    private Long waitSeconds;
    private Long intervalSeconds;
    /** 本次下发的验证码位数 (前端据此判断输入到第几位自动提交) */
    private Integer codeLength;

    public static SmsSendResultVO sent(long intervalSeconds, int codeLength) {
        SmsSendResultVO vo = new SmsSendResultVO();
        vo.setSent(Boolean.TRUE);
        vo.setWaitSeconds(0L);
        vo.setIntervalSeconds(intervalSeconds);
        vo.setCodeLength(codeLength);
        return vo;
    }

    public static SmsSendResultVO rateLimited(long waitSeconds, long intervalSeconds) {
        SmsSendResultVO vo = new SmsSendResultVO();
        vo.setSent(Boolean.FALSE);
        vo.setWaitSeconds(waitSeconds);
        vo.setIntervalSeconds(intervalSeconds);
        return vo;
    }

    public Boolean getSent() { return sent; }
    public void setSent(Boolean sent) { this.sent = sent; }
    public Long getWaitSeconds() { return waitSeconds; }
    public void setWaitSeconds(Long waitSeconds) { this.waitSeconds = waitSeconds; }
    public Long getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Long intervalSeconds) { this.intervalSeconds = intervalSeconds; }
    public Integer getCodeLength() { return codeLength; }
    public void setCodeLength(Integer codeLength) { this.codeLength = codeLength; }
}
