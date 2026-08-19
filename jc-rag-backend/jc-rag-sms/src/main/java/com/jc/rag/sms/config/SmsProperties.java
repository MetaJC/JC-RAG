package com.jc.rag.sms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 短信服务配置 (前缀 jc-rag.sms).
 * <p>渠道切换: {@link #provider} = log | aliyun | tencent, 默认 log (验证码仅输出到服务端日志).
 * <p>开发测试后门: {@link #testEnabled} + {@link #testCode}, 仅限开发环境显式开启,
 * 校验时输入该固定验证码直接放行, 生产环境必须保持关闭.
 *
 * @author JC
 */
@ConfigurationProperties(prefix = "jc-rag.sms")
public class SmsProperties {

    /** 发送渠道: log (默认, 演示/开发) | aliyun | tencent */
    private String provider = "log";

    /** 验证码有效期 (秒), 默认 300 */
    private long codeTtlSeconds = 300L;

    /** 同一手机号发送间隔 (秒), 默认 60 */
    private long sendIntervalSeconds = 60L;

    /** 开发环境测试后门开关, 默认关闭; 开启后输入 testCode 直接通过校验 */
    private boolean testEnabled = false;

    /** 测试后门固定验证码 (仅 testEnabled=true 时生效) */
    private String testCode = "8888";

    /** 阿里云渠道配置 */
    private final Aliyun aliyun = new Aliyun();

    /** 腾讯云渠道配置 */
    private final Tencent tencent = new Tencent();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public long getCodeTtlSeconds() { return codeTtlSeconds; }
    public void setCodeTtlSeconds(long codeTtlSeconds) { this.codeTtlSeconds = codeTtlSeconds; }
    public long getSendIntervalSeconds() { return sendIntervalSeconds; }
    public void setSendIntervalSeconds(long sendIntervalSeconds) { this.sendIntervalSeconds = sendIntervalSeconds; }
    public boolean isTestEnabled() { return testEnabled; }
    public void setTestEnabled(boolean testEnabled) { this.testEnabled = testEnabled; }
    public String getTestCode() { return testCode; }
    public void setTestCode(String testCode) { this.testCode = testCode; }
    public Aliyun getAliyun() { return aliyun; }
    public Tencent getTencent() { return tencent; }

    /**
     * 阿里云短信配置 (模板需包含变量 ${code}).
     * <p>键名: jc-rag.sms.aliyun.{key-id, key-secret, sign, template-code, endpoint}
     */
    public static class Aliyun {
        /** 阿里云 AccessKey ID */
        private String keyId = "";
        /** 阿里云 AccessKey Secret */
        private String keySecret = "";
        /** 短信签名 (已在阿里云控制台审核通过) */
        private String sign = "";
        /** 短信模板 CODE (SMS_xxx, 模板变量需 ${code} 与 ${minutes}) */
        private String templateCode = "";
        /** OpenAPI 接入地址, 默认国内站 */
        private String endpoint = "dysmsapi.aliyuncs.com";

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getKeySecret() { return keySecret; }
        public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    /**
     * 腾讯云短信配置 (模板需包含变量 {code}).
     * <p>键名: jc-rag.sms.tencent.{key-id, key-secret, app-id, region, sign, template-code}
     */
    public static class Tencent {
        /** 腾讯云 SecretId */
        private String keyId = "";
        /** 腾讯云 SecretKey */
        private String keySecret = "";
        /** 短信应用 SdkAppId (1400 开头) */
        private String appId = "";
        /** 地域, 默认广州 */
        private String region = "ap-guangzhou";
        /** 短信签名 (已在腾讯云控制台审核通过) */
        private String sign = "";
        /** 短信模板 ID (控制台"模板 ID", 模板变量需 {1}=验证码 {2}=有效分钟) */
        private String templateCode = "";

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getKeySecret() { return keySecret; }
        public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getSign() { return sign; }
        public void setSign(String sign) { this.sign = sign; }
        public String getTemplateCode() { return templateCode; }
        public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    }
}
