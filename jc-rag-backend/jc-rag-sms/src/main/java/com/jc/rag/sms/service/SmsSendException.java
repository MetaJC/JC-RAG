package com.jc.rag.sms.service;

/**
 * 短信渠道发送失败异常.
 *
 * @author JC
 */
public class SmsSendException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SmsSendException(String message) {
        super(message);
    }

    public SmsSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
