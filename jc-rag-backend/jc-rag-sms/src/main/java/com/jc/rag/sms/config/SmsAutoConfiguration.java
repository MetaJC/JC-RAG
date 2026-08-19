package com.jc.rag.sms.config;

import com.jc.rag.sms.service.SmsCodeService;
import com.jc.rag.sms.service.SmsService;
import com.jc.rag.sms.service.impl.AliyunSmsServiceImpl;
import com.jc.rag.sms.service.impl.LogSmsServiceImpl;
import com.jc.rag.sms.service.impl.SmsCodeServiceImpl;
import com.jc.rag.sms.service.impl.TencentSmsServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 短信模块自动装配.
 * <p>通过 META-INF/spring/...AutoConfiguration.imports 注册, 不依赖启动类组件扫描
 * (user / pro-app 启动类仅扫描 com.jc.rag.user / com.jc.rag.common).
 * <p>渠道选择: jc-rag.sms.provider = log (默认, matchIfMissing) | aliyun | tencent.
 *
 * @author JC
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsAutoConfiguration {

    /** 日志演示渠道 (默认): 未配置 provider 或显式 log 时装配 */
    @Bean
    @ConditionalOnProperty(prefix = "jc-rag.sms", name = "provider", havingValue = "log", matchIfMissing = true)
    public SmsService logSmsService() {
        return new LogSmsServiceImpl();
    }

    /** 阿里云渠道 */
    @Bean
    @ConditionalOnProperty(prefix = "jc-rag.sms", name = "provider", havingValue = "aliyun")
    public SmsService aliyunSmsService(SmsProperties properties) {
        return new AliyunSmsServiceImpl(properties);
    }

    /** 腾讯云渠道 */
    @Bean
    @ConditionalOnProperty(prefix = "jc-rag.sms", name = "provider", havingValue = "tencent")
    public SmsService tencentSmsService(SmsProperties properties) {
        return new TencentSmsServiceImpl(properties);
    }

    /** 验证码生命周期服务 (限流 / 存储 / 校验), 复用主服务 Redis 连接 */
    @Bean
    @ConditionalOnMissingBean
    public SmsCodeService smsCodeService(SmsService smsService, SmsProperties properties,
                                         StringRedisTemplate redis) {
        return new SmsCodeServiceImpl(smsService, properties, redis);
    }
}
