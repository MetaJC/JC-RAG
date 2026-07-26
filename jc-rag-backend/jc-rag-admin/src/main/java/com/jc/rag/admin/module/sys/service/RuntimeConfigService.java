package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.param.RuntimeConfigUpdateParam;
import com.jc.rag.admin.module.sys.model.vo.DirBrowseVO;
import com.jc.rag.admin.module.sys.model.vo.EmbeddingVerifyVO;
import com.jc.rag.admin.module.sys.model.vo.OllamaModelVO;
import com.jc.rag.admin.module.sys.model.vo.RuntimeConfigVO;
import com.jc.rag.admin.module.sys.model.vo.SystemResourceVO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 运行时配置 Service 接口 (合并 yml + sys_config)。
 * @author JC
 */
public interface RuntimeConfigService {

    RuntimeConfigVO buildResponse();

    OllamaModelVO listOllamaModels();

    @Transactional
    RuntimeConfigVO saveRuntimeConfig(RuntimeConfigUpdateParam request);

    Map<String, Object> buildProviderOverrideForPython();

    Map<String, Object> buildRagOverrideForPython();

    int resolveRagTopK();

    int resolveRagTopN();

    double resolveRagSimilarityThreshold();

    List<Map<String, Object>> detectDevices();

    SystemResourceVO getSystemResources();

    EmbeddingVerifyVO verifyEmbeddingModel(String modelPath);

    DirBrowseVO browseDirectory(String path);

    List<Map<String, Object>> listLocalModels();

    Map<String, Object> downloadLocalModel(String modelId);
}
