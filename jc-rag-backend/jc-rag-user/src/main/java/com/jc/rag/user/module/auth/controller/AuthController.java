package com.jc.rag.user.module.auth.controller;

import com.jc.rag.common.security.CaptchaService;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import com.jc.rag.user.module.auth.model.param.LoginParam;
import com.jc.rag.user.module.auth.model.param.RegisterParam;
import com.jc.rag.user.module.auth.model.vo.CaptchaStatusVO;
import com.jc.rag.user.module.auth.model.vo.CurrentUserVO;
import com.jc.rag.user.module.auth.model.vo.LoginResultVO;
import com.jc.rag.user.module.auth.model.vo.RegisterResultVO;
import com.jc.rag.user.module.auth.model.vo.SiteConfigVO;
import com.jc.rag.user.module.auth.service.SiteConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端公共 Auth Controller (走 jc-rag-user 服务)。
 * <p>账号密码登录: 连续失败 {@value #LOGIN_FAIL_THRESHOLD} 次后强制要求滑动验证, 登录成功清零。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** 连续登录失败阈值: 达到该次数后下一次必须先完成滑动验证 */
    public static final int LOGIN_FAIL_THRESHOLD = 3;

    private final AuthenticationManager authenticationManager;
    private final RbacUserMapper rbacUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final SiteConfigService siteConfigService;

    public AuthController(AuthenticationManager authenticationManager,
                          RbacUserMapper rbacUserMapper,
                          PasswordEncoder passwordEncoder,
                          CaptchaService captchaService,
                          SiteConfigService siteConfigService) {
        this.authenticationManager = authenticationManager;
        this.rbacUserMapper = rbacUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
        this.siteConfigService = siteConfigService;
    }

    @PostMapping("/login")
    public R<LoginResultVO> login(@Valid @RequestBody LoginParam param, HttpServletRequest request) {
        int failCount = getLoginFailCount(param.getUsername());
        boolean needCaptcha = failCount >= LOGIN_FAIL_THRESHOLD;

        if (needCaptcha) {
            if (!captchaService.consumeToken(param.getCaptchaToken())) {
                return R.ok(LoginResultVO.fail("CAPTCHA_REQUIRED", "请先完成滑动验证", failCount));
            }
        } else if (param.getCaptchaToken() != null && !param.getCaptchaToken().isEmpty()) {
            captchaService.consumeToken(param.getCaptchaToken());
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(param.getUsername(), param.getPassword()));
            SecurityContext context = new SecurityContextImpl(auth);
            SecurityContextHolder.setContext(context);
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            CoreUserDetails user = (CoreUserDetails) auth.getPrincipal();
            rbacUserMapper.markLoginSuccess(user.getUserId());
            return R.ok(LoginResultVO.ok(user.getUserId(), user.getUsername(), user.getNickname(), user.getUserType()));
        } catch (BadCredentialsException ex) {
            rbacUserMapper.incrementLoginFail(param.getUsername());
            int newCount = getLoginFailCount(param.getUsername());
            return R.ok(LoginResultVO.fail("BAD_CREDENTIALS", "用户名或密码错误", newCount));
        }
    }

    /**
     * 查询指定用户的滑动验证需求状态。
     * <p>供前端页面刷新后恢复"是否需要滑动验证": 失败次数达阈值保持出现,
     * 仅登录成功后由 markLoginSuccess 清零。用户不存在时返回不需要。
     */
    @GetMapping("/captcha-status")
    public R<CaptchaStatusVO> captchaStatus(@RequestParam(value = "username", required = false) String username) {
        if (username == null || username.isEmpty()) {
            return R.ok(CaptchaStatusVO.of(false, 0));
        }
        int failCount = getLoginFailCount(username);
        return R.ok(CaptchaStatusVO.of(failCount >= LOGIN_FAIL_THRESHOLD, failCount));
    }

    /**
     * 站点配置 (登录页等公开页面读取).
     * <p>返回注册开关/协议勾选强制性/品牌信息, 前端据此控制入口显隐与自动登录策略.
     */
    @GetMapping("/site-config")
    public R<SiteConfigVO> siteConfig() {
        return R.ok(siteConfigService.getSiteConfig());
    }

    @PostMapping("/register")
    public R<RegisterResultVO> register(@Valid @RequestBody RegisterParam param) {
        if (!siteConfigService.isRegisterOpen()) {
            return R.fail(403, "系统暂未开放注册");
        }
        Long exists = rbacUserMapper.selectIdByUsername(param.getUsername());
        if (exists != null) {
            return R.fail(409, "用户名已存在");
        }
        String encoded = passwordEncoder.encode(param.getPassword());
        String nickname = (param.getNickname() == null || param.getNickname().isEmpty()) ? param.getUsername() : param.getNickname();
        rbacUserMapper.insertNormalUser(param.getUsername(), encoded, nickname, param.getEmail());
        return R.ok(RegisterResultVO.ok());
    }

    @GetMapping("/me")
    public R<CurrentUserVO> currentUser(HttpServletRequest request) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CoreUserDetails user) {
            return R.ok(CurrentUserVO.of(user.getUserId(), user.getUsername(), user.getNickname(),
                user.getUserType(), user.isBypassAuth()));
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            var ctx = (org.springframework.security.core.context.SecurityContext)
                session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (ctx != null && ctx.getAuthentication() != null &&
                ctx.getAuthentication().getPrincipal() instanceof CoreUserDetails user) {
                return R.ok(CurrentUserVO.of(user.getUserId(), user.getUsername(), user.getNickname(),
                    user.getUserType(), user.isBypassAuth()));
            }
        }
        return R.ok(CurrentUserVO.anonymous());
    }

    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return R.ok();
    }

    /**
     * 查询指定用户当前失败次数 (用户不存在返回 0)。
     */
    private int getLoginFailCount(String username) {
        try {
            Integer c = rbacUserMapper.selectLoginFailCount(username);
            return c == null ? 0 : c;
        } catch (Exception e) {
            return 0;
        }
    }
}
