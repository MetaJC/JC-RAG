package com.jc.rag.admin.common.constants;

/**
 * RBAC 模块常量
 * @author JC
 */
public final class RbacConstants {

    private RbacConstants() {}

    /** 系统保留账号用户名 (可被 yml 覆盖) */
    public static final String DEFAULT_DEVELOPER_USERNAME = "developer";

    /** 默认初始密码 (可被 yml 覆盖) */
    public static final String DEFAULT_PASSWORD = "123456";

    /** 业务日志操作类型 */
    public static final class OpType {
        private OpType() {}
        public static final String CREATE_USER = "CREATE_USER";
        public static final String UPDATE_USER = "UPDATE_USER";
        public static final String RESET_PASSWORD = "RESET_PASSWORD";
        public static final String DELETE_USER = "DELETE_USER";
        public static final String CREATE_ROLE = "CREATE_ROLE";
        public static final String UPDATE_ROLE = "UPDATE_ROLE";
        public static final String DELETE_ROLE = "DELETE_ROLE";
        public static final String CREATE_PERMISSION = "CREATE_PERMISSION";
        public static final String UPDATE_PERMISSION = "UPDATE_PERMISSION";
        public static final String DELETE_PERMISSION = "DELETE_PERMISSION";
    }
}
