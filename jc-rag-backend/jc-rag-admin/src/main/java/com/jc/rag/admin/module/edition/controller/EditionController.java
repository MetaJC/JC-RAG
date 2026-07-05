package com.jc.rag.admin.module.edition.controller;

import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 版本信息 Controller (开源 / 商业版均存在).
 * <p>
 * 前端启动时调用此接口校验构建期版本与运行期后端版本是否一致.
 *
 * @author JC
 */
@RestController
@RequestMapping("/api/admin/edition")
@RequiredArgsConstructor
public class EditionController {

    @Value("${jc-rag.edition:oss}")
    private String edition;

    @Value("${jc-rag.edition-label:Community Edition}")
    private String editionLabel;

    /** 返回当前后端运行版本. */
    @GetMapping
    public R<Map<String, String>> getEdition() {
        Map<String, String> data = new HashMap<>();
        data.put("edition", edition);
        data.put("label", editionLabel);
        return R.ok(data);
    }
}
