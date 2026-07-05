package com.jc.rag.admin.module.sys.model.param;

import com.jc.rag.common.model.base.BaseParam;
import com.jc.rag.common.web.page.PageRequest;

/**
 * 仪表盘查询入参。
 *
 * @author JC
 */
public class DashboardQueryParam extends BaseParam {
    private PageRequest pageRequest;
    private String timeRange;

    public PageRequest getPageRequest() { return pageRequest; }
    public void setPageRequest(PageRequest v) { this.pageRequest = v; }
    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String v) { this.timeRange = v; }
}
