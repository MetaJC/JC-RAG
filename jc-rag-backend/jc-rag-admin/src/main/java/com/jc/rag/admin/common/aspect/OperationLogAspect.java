package com.jc.rag.admin.common.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jc.rag.admin.common.annotation.OperationLog;
import com.jc.rag.admin.common.config.RbacDefaultsProperties;
import com.jc.rag.admin.module.sys.service.OperationLogService;
import com.jc.rag.domain.entity.sys.SysOperationLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 操作日志 AOP 切面
 * 拦截带 @OperationLog 注解的方法, 自动捕获并持久化
 * @author JC
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final RbacDefaultsProperties rbacDefaults;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.jc.rag.admin.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                saveLog(pjp, result, error, cost);
            } catch (Exception ex) {
                log.error("[OperationLog] 保存失败: {}", ex.getMessage(), ex);
            }
        }
    }

    private void saveLog(ProceedingJoinPoint pjp, Object result, Throwable error, long costMs) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        OperationLog ann = method.getAnnotation(OperationLog.class);

        HttpServletRequest req = currentRequest();
        SysOperationLog entity = new SysOperationLog();
        entity.setModuleName(truncate(ann.module(), 64));
        entity.setOperationType(truncate(ann.action(), 64));
        entity.setOperationDesc(truncate(resolvePlaceholders(ann.description(), pjp.getArgs()), 500));
        entity.setRequestMethod(req != null && req.getMethod() != null ? req.getMethod() : "INTERNAL");
        entity.setRequestUri(truncate(req != null ? req.getRequestURI() : null, 255));
        entity.setRequestIp(truncate(req != null ? clientIp(req) : null, 64));
        entity.setUserAgent(truncate(req != null ? req.getHeader("User-Agent") : null, 512));
        entity.setCostMs(costMs);
        entity.setCreatedAt(LocalDateTime.now());

        // 操作人
        try {
            org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.jc.rag.admin.security.DbUserDetails u) {
                entity.setUserId(u.getUserId());
                entity.setUsername(u.getUsername());
            }
        } catch (Exception ignored) {}

        // 状态
        if (error == null) {
            entity.setStatus("SUCCESS");
        } else {
            entity.setStatus("FAILED");
            entity.setErrorMsg(truncate(error.getClass().getSimpleName() + ": " + error.getMessage(), 2000));
        }

        // 参数 (脱敏)
        if (ann.recordArgs()) {
            try {
                String argsJson = objectMapper.writeValueAsString(pjp.getArgs());
                entity.setArgs(maskJson(argsJson));
            } catch (Exception e) {
                entity.setArgs(Arrays.stream(pjp.getArgs())
                        .map(a -> a == null ? "null" : a.toString())
                        .collect(Collectors.joining(", ")));
            }
        }

        // 结果
        if (ann.recordResult() && result != null) {
            try {
                String r = objectMapper.writeValueAsString(result);
                if (r.length() > 2000) r = r.substring(0, 2000) + "...(truncated)";
                entity.setResult(r);
            } catch (Exception ignored) {}
        }

        operationLogService.saveAsync(entity);
    }

    private String maskJson(String json) {
        if (json == null) return null;
        String out = json;
        for (String k : rbacDefaults.getSensitiveFields()) {
            // "k":"v" -> "k":"******"
            String pattern = "(\"" + java.util.regex.Pattern.quote(k) + "\"\\s*:\\s*)\"[^\"]*\"";
            out = out.replaceAll(pattern, "$1\"******\"");
        }
        if (out.length() > 4000) out = out.substring(0, 4000) + "...(truncated)";
        return out;
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = req.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        return req.getRemoteAddr();
    }

    /** 防御性截断: 字符串超过 max 长度时按 UTF-8 边界安全截断, 避免 DB 字段超长. */
    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        int end = max;
        while (end > 0 && (s.charAt(end) & 0xC0) == 0x80) end--;
        return s.substring(0, end);
    }

    /**
     * 占位符替换: 把 description 中的 {0} / {0.name} 替换成 args 对应参数的值.
     * <p>语法: {index} 取 args[index].toString(); {index.field} 取 args[index].field 的值.
     * <p>field 找不到时返回空串, 不会抛异常.
     */
    private static String resolvePlaceholders(String desc, Object[] args) {
        if (desc == null || desc.isEmpty() || args == null) return desc;
        Pattern p = Pattern.compile("\\{(\\d+)(?:\\.([A-Za-z_]\\w*))?\\}");
        Matcher m = p.matcher(desc);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String replacement = "";
            try {
                int idx = Integer.parseInt(m.group(1));
                String field = m.group(2);
                if (idx >= 0 && idx < args.length) {
                    Object arg = args[idx];
                    if (field == null || field.isEmpty()) {
                        replacement = arg == null ? "null" : arg.toString();
                    } else if (arg != null) {
                        Field f = findField(arg.getClass(), field);
                        if (f != null) {
                            f.setAccessible(true);
                            Object v = f.get(arg);
                            replacement = v == null ? "" : v.toString();
                        }
                    }
                }
            } catch (Exception ignored) {
                // 占位符解析失败保留空串, 避免打断日志保存
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
