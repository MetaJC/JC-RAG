package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.module.sys.model.vo.ServiceHealthItemVO;
import com.jc.rag.admin.module.sys.service.SystemHealthService;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统服务健康 Controller (SpringBoot / Python RAG / Ollama / ES / MinIO)。
 * <p>路径: /api/sys/health/services
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/health/services")
@RequiredArgsConstructor
public class SysHealthController {

    private final SystemHealthService systemHealthService;

    @GetMapping
    public R<List<ServiceHealthItemVO>> get() {
        return R.ok(systemHealthService.collectHealthItems());
    }
}
