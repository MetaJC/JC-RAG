package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.ModelProviderSaveParam;
import com.jc.rag.admin.module.sys.model.param.ModelProviderTestParam;
import java.util.List;
import java.util.Map;

public interface ModelProviderConfigService {
    List<Map<String, Object>> list();
    Map<String, Object> save(ModelProviderSaveParam param);
    void delete(Long id);
    List<String> refreshModels(Long id);
    Map<String, Object> testConnection(ModelProviderTestParam param);
    void activate(Long id);
}
