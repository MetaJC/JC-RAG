/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


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
