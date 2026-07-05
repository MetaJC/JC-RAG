package com.jc.rag.admin.module.sys.service.impl;

import com.jc.rag.admin.module.sys.model.param.DictItemCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictQueryParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeCreateParam;
import com.jc.rag.admin.module.sys.model.param.DictTypeQueryParam;
import com.jc.rag.admin.module.sys.model.vo.DictItemVO;
import com.jc.rag.admin.module.sys.model.vo.DictTypeVO;
import com.jc.rag.admin.module.sys.service.DictService;
import com.jc.rag.common.web.page.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 字典管理实现 - TODO 业务待实现
 * @author JC
 */
@Slf4j
@Service
public class DictServiceImpl implements DictService {

    // TODO: 业务待实现 - 接入 sys_dict_type / sys_dict_item 表

    @Override
    public PageResponse<DictTypeVO> pageDictTypes(DictTypeQueryParam param) {
        return PageResponse.of(Collections.<DictTypeVO>emptyList(), 0L, param.getPageNum(), param.getPageSize());
    }

    @Override
    public PageResponse<DictItemVO> pageDictItems(DictQueryParam param) {
        return PageResponse.of(Collections.<DictItemVO>emptyList(), 0L, param.getPageNum(), param.getPageSize());
    }

    @Override
    public Long createDictType(DictTypeCreateParam param) {
        // TODO: 业务待实现
        log.info("[TODO] createDictType: {}", param.getTypeCode());
        return 0L;
    }

    @Override
    public Long createDictItem(DictItemCreateParam param) {
        // TODO: 业务待实现
        log.info("[TODO] createDictItem: {}.{}", param.getTypeCode(), param.getItemCode());
        return 0L;
    }
}
