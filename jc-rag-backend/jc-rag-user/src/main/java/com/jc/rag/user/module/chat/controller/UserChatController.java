package com.jc.rag.user.module.chat.controller;

import com.jc.rag.common.enums.ResultCode;
import com.jc.rag.common.exception.AssertException;
import com.jc.rag.common.security.CoreUserDetails;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.user.module.chat.model.param.ChatAskParam;
import com.jc.rag.user.module.chat.model.param.SessionCreateParam;
import com.jc.rag.user.module.chat.model.param.SessionPinParam;
import com.jc.rag.user.module.chat.model.param.SessionRenameParam;
import com.jc.rag.user.module.chat.model.param.UserChatSessionQueryParam;
import com.jc.rag.user.module.chat.model.vo.AskResponseVO;
import com.jc.rag.user.module.chat.model.vo.ChatMessageVO;
import com.jc.rag.user.module.chat.model.vo.ChatSessionVO;
import com.jc.rag.user.module.chat.model.vo.KbOptionVO;
import com.jc.rag.user.module.chat.model.vo.KbSelectConfigVO;
import com.jc.rag.user.module.chat.model.vo.ModelInfoVO;
import com.jc.rag.user.module.chat.service.UserChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * C 端问答中心 Controller。
 * <p>路径: /api/chat, 与 admin 端 ChatController 区分 (admin 端调用 ChatService, C 端调用 UserChatService)。
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/chat")
public class UserChatController {

    private final UserChatService chatService;

    public UserChatController(UserChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/sessions")
    public R<PageResponse<ChatSessionVO>> listSessions(
            Authentication auth,
            UserChatSessionQueryParam query) {
        return R.ok(chatService.listSessions(getUserId(auth), query));
    }

    @PostMapping("/sessions")
    public R<ChatSessionVO> createSession(@Valid @RequestBody SessionCreateParam param, Authentication auth) {
        return R.ok(chatService.createSession(getUserId(auth), param.getSessionTitle(), param.getKbId()));
    }

    @PutMapping("/sessions/{id}")
    public R<ChatSessionVO> renameSession(@PathVariable Long id, @Valid @RequestBody SessionRenameParam param, Authentication auth) {
        return R.ok(chatService.renameSession(getUserId(auth), id, param.getSessionTitle()));
    }

    @PostMapping("/sessions/{id}/pin")
    public R<ChatSessionVO> pinSession(@PathVariable Long id, @Valid @RequestBody SessionPinParam param, Authentication auth) {
        return R.ok(chatService.pinSession(getUserId(auth), id, Boolean.TRUE.equals(param.getPinned())));
    }

    @GetMapping("/sessions/{id}/messages")
    public R<List<ChatMessageVO>> listMessages(@PathVariable Long id, Authentication auth) {
        return R.ok(chatService.listMessages(getUserId(auth), id));
    }

    @PostMapping("/sessions/{id}/ask")
    public R<AskResponseVO> ask(@PathVariable Long id, @Valid @RequestBody ChatAskParam param, Authentication auth) {
        return R.ok(chatService.ask(getUserId(auth), id, param.getQuestion(), param.getAgentId(), param.getCustomAgentId()));
    }

    @PostMapping(value = "/sessions/{id}/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@PathVariable Long id, @Valid @RequestBody ChatAskParam param, Authentication auth) {
        return chatService.askStream(getUserId(auth), id, param.getQuestion(),
            Boolean.TRUE.equals(param.getDeepThinking()), param.getAgentId(), param.getCustomAgentId(),
            param.getKbId(), param.getAttachments());
    }

    @DeleteMapping("/sessions/{id}")
    public R<BaseOpResultVO> deleteSession(@PathVariable Long id, Authentication auth) {
        return R.ok(chatService.deleteSession(getUserId(auth), id));
    }

    @GetMapping("/knowledge-bases")
    public R<List<KbOptionVO>> listKnowledgeBases() {
        return R.ok(chatService.listDefaultKnowledgeBases());
    }

    /** C 端知识库选择配置: mode = FIXED / SELECTABLE / EMPTY */
    @GetMapping("/knowledge-bases/config")
    public R<KbSelectConfigVO> getKbSelectConfig() {
        return R.ok(chatService.getKbSelectConfig());
    }

    /**
     * 获取当前生效的模型信息（用于前端判断多模态等能力）。
     *
     * @author JC
     */
    @GetMapping("/model-info")
    public R<ModelInfoVO> getModelInfo(Authentication auth) {
        return R.ok(chatService.getModelInfo(getUserId(auth)));
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof CoreUserDetails userDetails)) {
            throw new AssertException(ResultCode.UNAUTHORIZED, "auth.session.invalid");
        }
        return userDetails.getUserId();
    }
}
