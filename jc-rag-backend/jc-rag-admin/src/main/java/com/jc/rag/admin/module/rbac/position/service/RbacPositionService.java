package com.jc.rag.admin.module.rbac.position.service;

import com.jc.rag.admin.module.rbac.position.model.param.RbacPositionQueryParam;
import com.jc.rag.admin.module.rbac.position.model.param.RbacPositionSaveParam;
import com.jc.rag.admin.module.rbac.position.model.vo.RbacPositionVO;
import com.jc.rag.common.model.base.BaseService;
import com.jc.rag.common.web.page.PageResponse;
import com.jc.rag.domain.entity.rbac.RbacPositionEntity;

import java.util.List;

/**
 * 岗位管理 Service (RBAC 组织维度)。
 * <p>继承通用 BaseService 获得 MP 通用 CRUD, 本接口仅声明岗位管理特有业务方法。
 *
 * @author JC
 */
public interface RbacPositionService extends BaseService<RbacPositionEntity> {

    /**
     * 分页岗位列表 (VO 形式)。
     */
    PageResponse<RbacPositionVO> pageList(RbacPositionQueryParam query);

    /**
     * 岗位详情 (含部门名)。
     */
    RbacPositionVO getDetail(Long id);

    /**
     * 全量启用岗位 (用于下拉框等场景)。
     */
    List<RbacPositionVO> listActive();

    /**
     * 创建岗位。
     */
    Long create(RbacPositionSaveParam param);

    /**
     * 更新岗位。
     * @return true=成功 false=失败
     */
    boolean update(Long id, RbacPositionSaveParam param);

    /**
     * 删除岗位。
     * @return true=成功 false=失败
     */
    boolean delete(Long id);

    /**
     * 切换状态 (仅改 status)。
     * @return true=成功 false=失败
     */
    boolean updateStatus(Long id, Integer status);
}
