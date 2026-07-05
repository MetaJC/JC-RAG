package com.jc.rag.admin.module.sys.controller;

import com.jc.rag.admin.module.sys.model.vo.DashboardVO;
import com.jc.rag.admin.module.sys.service.DashboardService;
import com.jc.rag.common.web.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard Controller
 * @author JC
 */
@Slf4j
@RestController
@RequestMapping("/api/sys/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public R<DashboardVO> get() {
        return R.ok(dashboardService.getDashboard());
    }
}
