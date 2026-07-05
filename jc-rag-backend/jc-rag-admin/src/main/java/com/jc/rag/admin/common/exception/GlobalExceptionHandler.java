package com.jc.rag.admin.common.exception;

import com.jc.rag.common.exception.BizException;
import com.jc.rag.common.exception.DeveloperGuardException;
import com.jc.rag.domain.entity.sys.SysExceptionLog;
import com.jc.rag.domain.mapper.sys.SysExceptionLogMapper;
import com.jc.rag.common.web.R;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 * <p>所有未捕获的 Exception 都会同步落地 sys_exception_log，供「日志审计-异常日志」tab 展示。
 * <p>业务异常（BizException 及其子类）只打 warn 日志，不落 sys_exception_log。
 * @author JC
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final SysExceptionLogMapper exceptionLogMapper;
    private final MessageSource messageSource;

    public GlobalExceptionHandler(SysExceptionLogMapper exceptionLogMapper, MessageSource messageSource) {
        this.exceptionLogMapper = exceptionLogMapper;
        this.messageSource = messageSource;
    }

    /**
     * 保留账号保护异常 - 单独捕获，warn 日志便于审计追溯
     */
    @ExceptionHandler(DeveloperGuardException.class)
    public R<?> handleDeveloperGuard(DeveloperGuardException e, HttpServletRequest req) {
        log.warn("[保留账号保护] {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 业务异常基类兜底 - 适用于所有 BizException 子类。
     * <p>通过 {@link MessageSource} 把 {@code messageKey} + 占位符参数解析为最终提示消息。
     */
    @ExceptionHandler(BizException.class)
    public R<?> handleBizException(BizException e, HttpServletRequest req) {
        String msg = e.resolveMessage(messageSource, LocaleContextHolder.getLocale());
        log.warn("[业务异常] {} {}: code={} msg={}", req.getMethod(), req.getRequestURI(), e.getCode(), msg);
        return R.fail(e.getCode(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest req) {
        log.warn("[参数异常] {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return R.fail(400, e.getMessage());
    }

    /**
     * @RequestBody @Valid 校验失败：聚合成可读信息，按 400 业务码返回。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<?> handleValidation(MethodArgumentNotValidException e, HttpServletRequest req) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("[参数校验失败] {} {}: {}", req.getMethod(), req.getRequestURI(), detail);
        return R.fail(400, "参数校验失败: " + detail);
    }

    /**
     * 请求体 JSON 解析失败：单独捕获，避免被泛化为 500 系统异常。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<?> handleNotReadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        log.warn("[请求体解析失败] {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        String msg = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        return R.fail(400, "请求体格式错误: " + msg);
    }

    /**
     * 未登录 / 会话失效：返回真实 401，触发前端 http.ts 401 拦截器跳转登录页。
     * 必须在 @ExceptionHandler(Exception.class) 之前。
     */
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<R<?>> handleUnauthenticated(InsufficientAuthenticationException e, HttpServletRequest req) {
        log.warn("[未鉴权] {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(R.fail(401, e.getMessage()));
    }

    /**
     * 鉴权拒绝：PermissionInterceptor 抛出的"无 permCode"等场景，返回 403。
     * 必须在 @ExceptionHandler(Exception.class) 之前。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<?>> handleAccessDenied(AccessDeniedException e, HttpServletRequest req) {
        log.warn("[鉴权拒绝] {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(R.fail(403, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public R<?> handleException(Exception e, HttpServletRequest req) {
        log.error("[系统异常] {} {}", req.getMethod(), req.getRequestURI(), e);
        persistException(e, req, "SYSTEM");
        return R.fail(500, "系统异常: " + e.getMessage());
    }

    /**
     * 异步落库 sys_exception_log，避免主链路被日志写入阻塞。
     * 注意：这里用 @Async 标注的私有方法调用由 Spring 代理失效（自调用不生效），
     * 所以直接同步写入；写表逻辑简单、且 sys_exception_log 写入是 O(1) 不会成为瓶颈。
     */
    private void persistException(Exception e, HttpServletRequest req, String moduleName) {
        try {
            SysExceptionLog logRow = new SysExceptionLog();
            logRow.setTraceId(UUID.randomUUID().toString().replace("-", ""));
            logRow.setModuleName(moduleName);
            logRow.setExceptionType(e.getClass().getName());
            String message = e.getMessage();
            if (message == null) {
                message = "未知异常";
            } else if (message.length() > 1000) {
                message = message.substring(0, 1000);
            }
            logRow.setExceptionMessage(message);
            // stack 截前 4000 字符，避免巨型堆栈撑爆行
            String stack = stackToString(e);
            if (stack.length() > 4000) {
                stack = stack.substring(0, 4000);
            }
            logRow.setStackTrace(stack);
            logRow.setCreatedAt(LocalDateTime.now());
            exceptionLogMapper.insert(logRow);
        } catch (Exception writeError) {
            // 写异常日志失败不能影响主流程
            GlobalExceptionHandler.log.error("[异常日志写入失败] {}", writeError.getMessage(), writeError);
        }
    }

    private static String stackToString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append('\n');
        for (StackTraceElement el : t.getStackTrace()) {
            sb.append("  at ").append(el).append('\n');
            if (sb.length() > 8000) {
                sb.append("  ...(truncated)");
                break;
            }
        }
        return sb.toString();
    }
}

