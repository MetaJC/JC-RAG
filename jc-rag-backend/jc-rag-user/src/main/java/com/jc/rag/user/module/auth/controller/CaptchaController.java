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


package com.jc.rag.user.module.auth.controller;

import com.jc.rag.common.security.CaptchaService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.CaptchaChallengeVO;
import com.jc.rag.common.web.vo.CaptchaCheckVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 滑动验证码 Controller (user 端)。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/auth/captcha")
public class CaptchaController {

    private static final Logger log = Logger.getLogger(CaptchaController.class.getName());

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /** 颁发挑战 */
    @GetMapping("/challenge")
    public R<CaptchaChallengeVO> challenge() {
        String id = captchaService.issueChallenge();
        CaptchaChallengeVO vo = new CaptchaChallengeVO();
        vo.setChallengeId(id);
        vo.setIssuedAt(System.currentTimeMillis());
        return R.ok(vo);
    }

    /** 校验行为 + 颁发 token */
    @PostMapping("/check")
    public R<CaptchaCheckVO> check(@RequestBody CaptchaCheckRequest body) {
        String challengeId = body.getChallengeId();
        Long durationMs = body.getDurationMs();

        CaptchaService.VerifyResult vr = captchaService.verifyAndIssueToken(
            challengeId, durationMs == null ? 0L : durationMs
        );
        if (vr.success) {
            log.log(Level.INFO, "[captcha] 验证通过 challengeId=" + challengeId);
        } else {
            log.log(Level.WARNING, "[captcha] 校验失败: " + vr.message + " challengeId=" + challengeId);
        }

        CaptchaCheckVO vo = new CaptchaCheckVO();
        vo.setSuccess(vr.success);
        vo.setMessage(vr.message);
        vo.setCaptchaToken(vr.token);
        vo.setExpireInMs(vr.expireInMs);
        return R.ok(vo);
    }
}
