package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.SysLegalDocSaveParam;
import com.jc.rag.admin.module.sys.model.vo.SysLegalDocVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户协议 / 隐私政策 Service。
 * <p>每种 docType 全表唯一, 业务上只有一条记录 (草稿/已发布切换)。
 *
 * @author JC
 */
public interface SysLegalDocService {

    /** 列表: 所有 docType (草稿+已发布) */
    List<SysLegalDocVO> listAll();

    /** 单条详情 (草稿/已发布都返回) */
    SysLegalDocVO getByDocType(String docType);

    /**
     * 保存 (草稿或发布) - 同一 docType 唯一
     * @return true=成功 false=失败
     */
    boolean save(SysLegalDocSaveParam param, Long userId);

    /**
     * 发布: 把指定 docType 置为 PUBLISHED, 记录发布时间+发布人
     * @return true=成功 false=失败
     */
    boolean publish(String docType, Long userId);

    /** 解析上传的 word/markdown 文件为 Markdown 字符串, 仅做转换不入库 */
    String parseUploadedFile(MultipartFile file);
}
