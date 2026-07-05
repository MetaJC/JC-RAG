package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.vo.ServiceHealthItemVO;

import java.util.List;

/**
 * 系统健康检查 Service 接口 (SpringBoot / Python RAG / Ollama / ES / MinIO)。
 * @author JC
 */
public interface SystemHealthService {

    List<ServiceHealthItemVO> collectHealthItems();
}
