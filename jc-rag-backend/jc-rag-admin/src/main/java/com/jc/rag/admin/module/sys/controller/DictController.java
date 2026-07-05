package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.module.sys.model.param.DictItemCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictQueryParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeQueryParam;
import com.jc.rag.admin.module.sys.model.vo.DictItemVO;
import com.jc.rag.admin.module.sys.model.vo.DictTypeVO;
import com.jc.rag.admin.module.sys.service.DictService;
import com.jc.rag.common.web.R;
import com.jc.rag.common.web.page.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 字典管理 Controller - TODO 业务待实现
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    public R<PageResponse<DictTypeVO>> pageTypes(DictTypeQueryParam param) {
        return R.ok(dictService.pageDictTypes(param));
    }

    @GetMapping("/items")
    public R<PageResponse<DictItemVO>> pageItems(DictQueryParam param) {
        return R.ok(dictService.pageDictItems(param));
    }

    @PostMapping("/types")
    public R<Long> createType(@RequestBody DictTypeCreateParam param) {
        return R.ok(dictService.createDictType(param));
    }

    @PostMapping("/items")
    public R<Long> createItem(@RequestBody DictItemCreateParam param) {
        return R.ok(dictService.createDictItem(param));
    }
}
