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
