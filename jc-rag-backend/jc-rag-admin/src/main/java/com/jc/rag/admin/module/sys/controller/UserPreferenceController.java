package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.module.sys.model.param.UserPreferenceUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.UserPreferenceVO;
import com.jc.rag.admin.module.sys.service.UserPreferenceService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户偏好设置 Controller (齿轮 - 偏好设置弹窗)。
 * <p>路径: /api/sys/user-preference
 * <p>第一阶段仅实现: memoryFilePath (记忆文件位置)
 *
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/user-preference")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping
    public R<UserPreferenceVO> get(Authentication auth) {
        Long userId = currentUserId(auth);
        return R.ok(userPreferenceService.load(userId));
    }

    @PutMapping
    @OperationLog(module = "SYS", action = "UPDATE_USER_PREFERENCE", description = "更新个人偏好")
    public R<UserPreferenceVO> update(@RequestBody @Valid UserPreferenceUpdateParam param, Authentication auth) {
        Long userId = currentUserId(auth);
        return R.ok(userPreferenceService.update(userId, param));
    }

    private Long currentUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof DbUserDetails u) {
            return u.getUserId();
        }
        return null;
    }
}
