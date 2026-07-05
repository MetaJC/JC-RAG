package com.jc.rag.admin.module.sys.service;

import com.jc.rag.admin.module.sys.model.vo.DashboardVO;

/**
 * Dashboard 数据 Service
 * @author JC
 */
public interface DashboardService {

    /** 获取 Dashboard 聚合数据 */
    DashboardVO getDashboard();
}
