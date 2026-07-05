package com.jc.rag.admin.module.chat.controller;

import com.jc.rag.admin.module.chat.model.param.AdminChatSessionQueryParam;
import com.jc.rag.admin.module.chat.model.param.ChatAskParam;
import com.jc.rag.admin.module.chat.model.param.ChatSessionCreateParam;
import com.jc.rag.admin.module.chat.model.vo.AdminAskResponseVO;
import com.jc.rag.admin.module.chat.model.vo.AdminChatMessageVO;
import com.jc.rag.admin.module.chat.model.vo.AdminChatSessionVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.admin.module.chat.service.ChatService;
import com.jc.rag.admin.security.DbUserDetails;
import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 问答中心接口。
 * <p>提供会话管理、消息查询、提问（支持流式/非流式）等能力。
 * <p>统一使用 {@code R<VO>} 包装响应; SSE 流式接口因协议特殊, 直接返回 {@link SseEmitter}。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 列出会话列表（分页+搜索） */
    @GetMapping("/sessions")
    public R<PageResponse<AdminChatSessionVO>> listSessions(
            Authentication authentication,
            AdminChatSessionQueryParam query) {
        return R.ok(chatService.listSessions(currentUserId(authentication), query));
    }

    /** 创建会话 */
    @PostMapping("/sessions")
    public R<AdminChatSessionVO> createSession(@Valid @RequestBody ChatSessionCreateParam request, Authentication authentication) {
        return R.ok(chatService.createSession(currentUserId(authentication), request));
    }

    /** 查询会话消息列表 */
    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<AdminChatMessageVO>> listMessages(@PathVariable Long sessionId, Authentication authentication) {
        return R.ok(chatService.listMessages(currentUserId(authentication), sessionId));
    }

    /** 非流式提问 */
    @PostMapping("/sessions/{sessionId}/ask")
    public R<AdminAskResponseVO> ask(
        @PathVariable Long sessionId,
        @Valid @RequestBody ChatAskParam request,
        Authentication authentication
    ) {
        return R.ok(chatService.ask(currentUserId(authentication), sessionId, request));
    }

    /** 流式提问 (SSE, 唯一允许直接返回 SseEmitter 的场景) */
    @PostMapping("/sessions/{sessionId}/ask/stream")
    public SseEmitter askStream(
        @PathVariable Long sessionId,
        @Valid @RequestBody ChatAskParam request,
        Authentication authentication
    ) {
        return chatService.askStream(currentUserId(authentication), sessionId, request);
    }

    /** 删除当前用户自己的会话 */
    @DeleteMapping("/sessions/{sessionId}")
    public R<BaseOpResultVO> deleteSession(@PathVariable Long sessionId, Authentication authentication) {
        return R.ok(chatService.deleteSession(currentUserId(authentication), sessionId));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof DbUserDetails userDetails)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return userDetails.getUserId();
    }
}
