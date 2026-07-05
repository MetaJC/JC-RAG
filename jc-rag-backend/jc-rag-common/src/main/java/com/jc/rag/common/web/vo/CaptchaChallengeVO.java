package com.jc.rag.common.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * 滑动验证码 challenge 响应 VO (admin/user 通用)。
 * <p>前端用 challengeId + bgImage 渲染拼图, 拼图坐标由 verify 接口校验。
 *
 * @author JC
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaptchaChallengeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String challengeId;
    private String bgImage;
    private String puzzleImage;
    private Integer puzzleY;
    private Integer puzzleWidth;
    private Integer bgImageWidth;
    private Integer puzzleHeight;
    private Integer bgImageHeight;
    private Long issuedAt;

    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
    public String getBgImage() { return bgImage; }
    public void setBgImage(String bgImage) { this.bgImage = bgImage; }
    public String getPuzzleImage() { return puzzleImage; }
    public void setPuzzleImage(String puzzleImage) { this.puzzleImage = puzzleImage; }
    public Integer getPuzzleY() { return puzzleY; }
    public void setPuzzleY(Integer puzzleY) { this.puzzleY = puzzleY; }
    public Integer getPuzzleWidth() { return puzzleWidth; }
    public void setPuzzleWidth(Integer puzzleWidth) { this.puzzleWidth = puzzleWidth; }
    public Integer getBgImageWidth() { return bgImageWidth; }
    public void setBgImageWidth(Integer bgImageWidth) { this.bgImageWidth = bgImageWidth; }
    public Integer getPuzzleHeight() { return puzzleHeight; }
    public void setPuzzleHeight(Integer puzzleHeight) { this.puzzleHeight = puzzleHeight; }
    public Integer getBgImageHeight() { return bgImageHeight; }
    public void setBgImageHeight(Integer bgImageHeight) { this.bgImageHeight = bgImageHeight; }
    public Long getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Long issuedAt) { this.issuedAt = issuedAt; }
}
