package com.jc.rag.user.module.auth.controller;

import com.jc.rag.common.security.CaptchaService;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.sms.enums.SmsCodeLength;
import com.jc.rag.sms.service.SmsCodeService;
import com.jc.rag.user.module.auth.model.param.SmsLoginParam;
import com.jc.rag.user.module.auth.model.param.SmsSendParam;
import com.jc.rag.user.module.auth.model.vo.LoginResultVO;
import com.jc.rag.user.module.auth.model.vo.SmsSendResultVO;
import com.jc.rag.user.module.auth.service.SiteConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * 短信验证码登录 Controller (用户端)。
 * <p>链路: 滑动验证 → 发送验证码 (限流 60s 可配置, 默认 4 位纯数字) → 验证码登录/注册一体。
 * <ul>
 *   <li>POST /api/auth/sms/send: 消费滑动验证 token 后下发验证码, 回执携带验证码位数</li>
 *   <li>POST /api/auth/sms/login: 校验验证码; 注册开关开放且手机号未注册时自动注册后登录</li>
 * </ul>
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/auth/sms")
public class SmsAuthController {

    private final SmsCodeService smsCodeService;
    private final CaptchaService captchaService;
    private final RbacUserMapper rbacUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SiteConfigService siteConfigService;

    public SmsAuthController(SmsCodeService smsCodeService,
                             CaptchaService captchaService,
                             RbacUserMapper rbacUserMapper,
                             PasswordEncoder passwordEncoder,
                             SiteConfigService siteConfigService) {
        this.smsCodeService = smsCodeService;
        this.captchaService = captchaService;
        this.rbacUserMapper = rbacUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.siteConfigService = siteConfigService;
    }

    /**
     * 发送短信验证码。
     * <p>时序: 滑动验证有效性预检 (peek, 不消费) → 手机号限流预检 → 通过后
     * 才消费 captchaToken 并下发验证码, 避免预检失败白白烧掉一次性 token。
     */
    @PostMapping("/send")
    public R<SmsSendResultVO> send(@Valid @RequestBody SmsSendParam param) {
        long interval = smsCodeService.getSendIntervalSeconds();
        if (!captchaService.peekToken(param.getCaptchaToken())) {
            return R.fail(400, "请先完成滑动验证");
        }
        long remaining = smsCodeService.getRemainingInterval(param.getPhone());
        if (remaining > 0) {
            return R.ok(SmsSendResultVO.rateLimited(remaining, interval));
        }
        captchaService.consumeToken(param.getCaptchaToken());
        long wait = smsCodeService.sendCode(param.getPhone());
        // 回执携带验证码位数: 前端据此判断输入到第几位触发自动登录
        return wait > 0 ? R.ok(SmsSendResultVO.rateLimited(wait, interval))
                        : R.ok(SmsSendResultVO.sent(interval, SmsCodeLength.DEFAULT.getDigits()));
    }

    /**
     * 短信验证码登录 (注册开关开放时, 未注册手机号自动注册).
     */
    @PostMapping("/login")
    public R<LoginResultVO> login(@Valid @RequestBody SmsLoginParam param, HttpServletRequest request) {
        if (!smsCodeService.verifyAndConsume(param.getPhone(), param.getCode())) {
            return R.ok(LoginResultVO.fail("SMS_CODE_INVALID", "验证码错误或已过期", 0));
        }
        Map<String, Object> row = rbacUserMapper.selectAuthInfoByPhone(param.getPhone());
        boolean autoRegistered = false;
        if (row == null) {
            if (!siteConfigService.isRegisterOpen()) {
                return R.ok(LoginResultVO.fail("REGISTER_CLOSED", "当前手机号尚未注册，请联系管理员", 0));
            }
            autoRegistered = true;
            registerByPhone(param.getPhone());
            row = rbacUserMapper.selectAuthInfoByPhone(param.getPhone());
        }
        CoreUserDetails user = new CoreUserDetails(
                ((Number) row.get("id")).longValue(),
                (String) row.get("username"),
                (String) row.get("password_hash"),
                (String) row.get("nickname"),
                (String) row.get("user_type"),
                false);

        SecurityContext context = new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        rbacUserMapper.markLoginSuccess(user.getUserId());
        LoginResultVO vo = LoginResultVO.ok(user.getUserId(), user.getUsername(), user.getNickname(), user.getUserType());
        if (autoRegistered) {
            vo.setMessage("已自动注册并登录");
        }
        return R.ok(vo);
    }

    /**
     * 按手机号自动注册: username 即手机号 (借 username 唯一性兜底),
     * 密码哈希为随机值 (该账号只能通过短信验证码登录)。
     */
    private void registerByPhone(String phone) {
        String randomHash = passwordEncoder.encode(UUID.randomUUID().toString());
        String nickname = "用户" + phone.substring(phone.length() - 4);
        rbacUserMapper.insertSmsUser(phone, randomHash, nickname, phone);
    }
}
