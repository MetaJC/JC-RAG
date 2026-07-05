package com.jc.rag.user.module.user.controller;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.user.module.user.model.param.UserApiKeySaveParam;
import com.jc.rag.user.module.user.model.param.UserApiKeyToggleParam;
import com.jc.rag.user.module.user.model.vo.UserApiKeyVO;
import com.jc.rag.user.module.user.service.UserApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户自定义 API Key Controller (C 端个人中心)。
 * <p>路径: /api/user/api-keys, 与 admin 端 SysApiKeyController 区分。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/user/api-keys")
@RequiredArgsConstructor
public class UserApiKeyController {

    private final UserApiKeyService userApiKeyService;

    @GetMapping
    public R<List<UserApiKeyVO>> list(Authentication auth) {
        return R.ok(userApiKeyService.listByUserId(currentUserId(auth)));
    }

    @PostMapping
    public R<BaseOpResultVO> save(@RequestBody UserApiKeySaveParam param, Authentication auth) {
        return R.ok(userApiKeyService.saveOrUpdate(currentUserId(auth), param));
    }

    @DeleteMapping("/{id}")
    public R<BaseOpResultVO> delete(@PathVariable Long id, Authentication auth) {
        return R.ok(userApiKeyService.remove(currentUserId(auth), id));
    }

    @PostMapping("/{id}/toggle")
    public R<BaseOpResultVO> toggle(@PathVariable Long id, @RequestBody UserApiKeyToggleParam param, Authentication auth) {
        return R.ok(userApiKeyService.toggle(currentUserId(auth), id, param.getActive() != null && param.getActive()));
    }

    private static Long currentUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CoreUserDetails cu)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return cu.getUserId();
    }
}
