package com.jc.rag.sms.service.impl;

import com.jc.rag.sms.config.SmsProperties;
import com.jc.rag.sms.service.SmsSendException;
import com.jc.rag.sms.service.SmsService;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20210111.models.SendStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 腾讯云短信渠道实现 (provider=tencent).
 * <p>模板需包含变量 {code}; 手机号按腾讯云规范补 +86 前缀.
 *
 * @author JC
 */
public class TencentSmsServiceImpl implements SmsService {

    private static final Logger log = LoggerFactory.getLogger(TencentSmsServiceImpl.class);

    private final SmsClient client;
    private final SmsProperties.Tencent cfg;

    public TencentSmsServiceImpl(SmsProperties properties) {
        this.cfg = properties.getTencent();
        Credential credential = new Credential(cfg.getKeyId(), cfg.getKeySecret());
        this.client = new SmsClient(credential, cfg.getRegion());
    }

    @Override
    public void sendCode(String phone, String code, long ttlMinutes) {
        try {
            SendSmsRequest request = new SendSmsRequest();
            request.setSmsSdkAppId(cfg.getAppId());
            request.setSignName(cfg.getSign());
            request.setTemplateId(cfg.getTemplateCode());
            request.setPhoneNumberSet(new String[]{"+86" + phone});
            request.setTemplateParamSet(new String[]{code, String.valueOf(ttlMinutes)});
            SendSmsResponse response = client.SendSms(request);
            SendStatus[] statusSet = response.getSendStatusSet();
            if (statusSet == null || statusSet.length == 0) {
                throw new SmsSendException("腾讯云短信发送失败: 无回执");
            }
            if (!"Ok".equals(statusSet[0].getCode())) {
                throw new SmsSendException("腾讯云短信发送失败: code=" + statusSet[0].getCode()
                        + ", message=" + statusSet[0].getMessage());
            }
            log.info("[SMS] 腾讯云验证码已发送: phone={}", phone);
        } catch (SmsSendException e) {
            throw e;
        } catch (Exception e) {
            throw new SmsSendException("腾讯云短信发送异常: " + e.getMessage(), e);
        }
    }
}
