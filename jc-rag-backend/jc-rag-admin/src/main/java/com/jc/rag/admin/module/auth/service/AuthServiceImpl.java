package com.jc.rag.admin.module.auth.service;

import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.module.auth.model.param.ChangePasswordParam;
import com.jc.rag.admin.module.auth.model.param.LoginParam;
import com.jc.rag.admin.module.auth.model.vo.AuthUserVO;
import com.jc.rag.admin.module.rbac.service.RbacMenuService;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.security.CaptchaService;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.domain.mapper.rbac.RbacRoleMapper;
import com.jc.rag.domain.mapper.rbac.RbacUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * AuthService 实现
 *
 * @author JC
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;
    private final RbacUserMapper userMapper;
    private final RbacRoleMapper roleMapper;
    private final RbacMenuService menuService;
    private final CaptchaService captchaService;
    private final RbacDefaultsProperties rbacDefaults;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            OperationLogService operationLogService,
            RbacUserMapper userMapper,
            RbacRoleMapper roleMapper,
            RbacMenuService menuService,
            CaptchaService captchaService,
            RbacDefaultsProperties rbacDefaults) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuService = menuService;
        this.captchaService = captchaService;
        this.rbacDefaults = rbacDefaults;
    }

    /**
     * 登录并创建 Session。
     */
    @Override
    public AuthUserVO login(LoginParam request, HttpServletRequest httpServletRequest) {
        if (!captchaService.consumeToken(request.getCaptchaToken())) {
            throw new BizException("请先完成滑动验证");
        }
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        // 校验: 至少拥有一个 is_admin=1 的角色, 才有权登录管理端 (developer 账号绕过)
        DbUserDetails userDetails = (DbUserDetails) authentication.getPrincipal();
        assertAdminLoginAllowed(userDetails);

        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpServletRequest.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        operationLogService.log("登录鉴权", "LOGIN", "用户登录:" + userDetails.getUsername(), "SUCCESS");
        return toResponse(userDetails);
    }

    /**
     * 登录管理端准入校验: 用户必须拥有至少一个 is_admin=1 且启用的角色。
     * <p>developer 兜底账号不受此限制, 永远可登录。
     */
    private void assertAdminLoginAllowed(DbUserDetails userDetails) {
        if (rbacDefaults.getDeveloperUsername().equalsIgnoreCase(userDetails.getUsername())) {
            return;
        }
        long count = roleMapper.countAdminRoleByUserId(userDetails.getUserId());
        if (count <= 0) {
            operationLogService.log("登录鉴权", "LOGIN_DENY",
                    "用户无管理端角色: " + userDetails.getUsername(), "DENIED");
            throw new BizException("当前账号未授权登录管理端, 请联系管理员");
        }
    }

    /**
     * 获取当前登录用户信息。
     */
    @Override
    public AuthUserVO currentUser() {
        DbUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            throw new InsufficientAuthenticationException("未登录或会话已失效");
        }
        return toResponse(userDetails);
    }

    /**
     * 退出登录并销毁 Session。
     */
    @Override
    public BaseOpResultVO logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return BaseOpResultVO.success("已退出登录");
    }

    /**
     * 修改当前用户密码。
     */
    @Override
    public BaseOpResultVO changePassword(ChangePasswordParam request) {
        DbUserDetails userDetails = getCurrentUserDetails();
        if (userDetails == null) {
            throw new InsufficientAuthenticationException("未登录或会话已失效");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDetails.getUsername(), request.getOldPassword()));
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePassword(userDetails.getUserId(), encodedPassword);
        operationLogService.log("登录鉴权", "CHANGE_PASSWORD",
                "修改密码:" + userDetails.getUsername(), "SUCCESS");
        return BaseOpResultVO.success(userDetails.getUserId(), "密码已修改");
    }

    private DbUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof DbUserDetails)) {
            return null;
        }
        return (DbUserDetails) principal;
    }

    private AuthUserVO toResponse(DbUserDetails userDetails) {
        AuthUserVO response = new AuthUserVO();
        response.setUserId(userDetails.getUserId());
        response.setUsername(userDetails.getUsername());
        response.setNickname(userDetails.getNickname());
        response.setUserType(userDetails.getUserType());
        response.setPermCodes(userDetails.getPermCodes());
        response.setMenus(menuService.listMenuTreeByUserId(userDetails.getUserId()));
        return response;
    }
}
