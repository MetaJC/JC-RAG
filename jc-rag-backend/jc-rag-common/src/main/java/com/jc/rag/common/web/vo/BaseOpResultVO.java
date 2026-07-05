package com.jc.rag.common.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 通用操作结果 VO (基类)。
 * <p>用于 删除 / 置顶 / 取消 / 终止 / 重命名 等单条/批量操作的统一返回结构.
 * <p>未使用的字段 (status/targetId/sessionId/extraCount) 会通过 {@code @JsonInclude(NON_NULL)} 自动隐藏,
 * 避免污染前端响应结构.
 *
 * <pre>
 * 推荐用法:
 *   BaseOpResultVO.success(sessionId, "操作成功")
 *   BaseOpResultVO.fail(sessionId, "操作失败原因")
 * </pre>
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseOpResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 操作是否成功 (true=成功, false=失败, null=未设置). */
    private Boolean success;

    /** 状态文案 (SUCCESS/FAILED/PARTIAL 等). */
    private String status;

    /** 目标对象 id (知识库/文档/任务等). */
    private Long targetId;

    /** 会话 id (会话类操作专用). */
    private Long sessionId;

    /** 附加计数 (删除条数/影响行数等). */
    private Integer extraCount;

    /** 操作描述 (成功消息或失败原因). */
    private String message;

    public BaseOpResultVO() {}

    /** 成功快捷构造. */
    public static BaseOpResultVO success(Long targetId, String message) {
        BaseOpResultVO vo = new BaseOpResultVO();
        vo.setSuccess(Boolean.TRUE);
        vo.setStatus("SUCCESS");
        vo.setTargetId(targetId);
        vo.setMessage(message);
        return vo;
    }

    /** 无目标 ID 的成功快捷构造 (e.g. 退出登录). */
    public static BaseOpResultVO success(String message) {
        return success(null, message);
    }

    /** 失败快捷构造. */
    public static BaseOpResultVO fail(Long targetId, String message) {
        BaseOpResultVO vo = new BaseOpResultVO();
        vo.setSuccess(Boolean.FALSE);
        vo.setStatus("FAILED");
        vo.setTargetId(targetId);
        vo.setMessage(message);
        return vo;
    }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Integer getExtraCount() { return extraCount; }
    public void setExtraCount(Integer extraCount) { this.extraCount = extraCount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
