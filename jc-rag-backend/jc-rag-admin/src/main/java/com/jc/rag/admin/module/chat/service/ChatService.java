package com.jc.rag.admin.module.chat.service;

import com.jc.rag.admin.module.chat.model.param.AdminChatSessionQueryParam;
import com.jc.rag.admin.module.chat.model.param.ChatAskParam;
import com.jc.rag.admin.module.chat.model.param.ChatSessionCreateParam;
import com.jc.rag.admin.module.chat.model.vo.AdminAskResponseVO;
import com.jc.rag.admin.module.chat.model.vo.AdminChatMessageVO;
import com.jc.rag.admin.module.chat.model.vo.AdminChatSessionVO;
import com.jc.rag.common.web.vo.BaseOpResultVO;
import com.jc.rag.common.web.page.PageRequest;
import com.jc.rag.common.web.page.PageResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * ChatService 接口。
 * <p>所有方法返回严格类型 VO, 禁止使用 {@code Map<String,Object>}。
 * <p>流式接口因协议特殊, 仍直接返回 {@link SseEmitter} (Controller 层不包装 R)。
 *
 * @author JC
 */
public interface ChatService {

    List<AdminChatSessionVO> listSessions(Long userId);

    PageResponse<AdminChatSessionVO> listSessions(Long userId, AdminChatSessionQueryParam query);

    @Transactional
    AdminChatSessionVO createSession(Long userId, ChatSessionCreateParam request);

    /** @deprecated 仅占位, 调用方必须传 userId 上下文, 见 {@link #listMessages(Long, Long)} */
    @Deprecated
    List<AdminChatMessageVO> listMessages(Long sessionId);

    List<AdminChatMessageVO> listMessages(Long userId, Long sessionId);

    @Transactional
    AdminAskResponseVO ask(Long userId, Long sessionId, ChatAskParam request);

    SseEmitter askStream(Long userId, Long sessionId, ChatAskParam request);

    @Transactional
    BaseOpResultVO deleteSession(Long userId, Long sessionId);
}
