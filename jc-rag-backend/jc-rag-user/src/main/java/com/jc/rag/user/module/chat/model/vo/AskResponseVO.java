package com.jc.rag.user.module.chat.model.vo;

/**
 * 非流式提问响应 VO。
 * <p>仅承载 answer 字段, 与流式 (SSE) 接口的协议保持形态对称, 便于前端按同一模型渲染。
 *
 * @author JC
 */
public class AskResponseVO {
    private String answer;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    @Override
    public String toString() {
        return "AskResponseVO{answer='" + (answer == null ? "" : answer.substring(0, Math.min(50, answer.length()))) + "...'}";
    }
}
