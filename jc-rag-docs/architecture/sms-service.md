# 短信服务 (jc-rag-sms)

> 独立 Maven 模块, 提供短信渠道抽象 + 验证码全生命周期管理, 当前用于 C 端"手机号验证码登录/注册一体"。
> 双版本共用: 代码位于社区版 `jc-rag-backend/jc-rag-sms/`, 商业版通过依赖传递复用, 无 Pro 专属分支。

## 模块定位

| 维度 | 说明 |
|------|------|
| 粒度 | 独立 Maven module (`jc-rag-backend/jc-rag-sms`), 非公共类、非独立微服务 |
| 装配 | Spring Boot 自动装配 (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), 宿主 `UserApplication` 组件扫描范围外也可生效 |
| 依赖方 | 当前仅 `jc-rag-user` (验证码登录); 后续 admin 或其他模块需要短信能力时直接加依赖即可 |
| 渠道策略 | 多家服务商可配置切换, `jc-rag.sms.provider` 决定装配哪个实现 |

## 渠道实现

| provider 值 | 实现类 | 说明 |
|-------------|--------|------|
| `log` (默认) | `LogSmsServiceImpl` | 演示渠道: 验证码仅输出到服务端日志, 零外部依赖, 开箱即用 |
| `aliyun` | `AliyunSmsServiceImpl` | 阿里云短信 (dysmsapi20170525 SDK) |
| `tencent` | `TencentSmsServiceImpl` | 腾讯云短信 (tencentcloud-sdk-java-sms) |

三个实现均按 `@ConditionalOnProperty(prefix="jc-rag.sms", name="provider", havingValue=...)` 条件装配, `log` 为 `matchIfMissing=true` 兜底。切换渠道只改配置, 无需改代码。

## 配置项 (`jc-rag-user/config/application.yml` + 环境分层)

| Key | 默认 | 说明 |
|-----|------|------|
| `jc-rag.sms.provider` | `log` | 渠道: log / aliyun / tencent |
| `jc-rag.sms.code-ttl-seconds` | 300 | 验证码有效期 (秒) |
| `jc-rag.sms.send-interval-seconds` | 60 | 同一手机号发送间隔 (秒), 即前端倒计时时长 |
| `jc-rag.sms.test-enabled` | false | 测试后门开关 (**仅限开发环境**, 生产必须 false) |
| `jc-rag.sms.test-code` | `8888` | 测试后门验证码, 开启后输入该码直接通过校验 |

### 服务商凭证 (多家可同时配置, provider 决定当前启用哪家)

| Key | 说明 |
|-----|------|
| `jc-rag.sms.aliyun.key-id` / `key-secret` | 阿里云 AccessKey ID / Secret |
| `jc-rag.sms.aliyun.sign` | 短信签名 (控制台已审核) |
| `jc-rag.sms.aliyun.template-code` | 模板 CODE (SMS_xxx, 变量 ${code} ${minutes}) |
| `jc-rag.sms.aliyun.endpoint` | OpenAPI 接入地址, 默认 dysmsapi.aliyuncs.com |
| `jc-rag.sms.tencent.key-id` / `key-secret` | 腾讯云 SecretId / SecretKey |
| `jc-rag.sms.tencent.app-id` | 短信应用 SdkAppId (1400 开头, YAML 中加引号) |
| `jc-rag.sms.tencent.region` | 地域, 默认 ap-guangzhou |
| `jc-rag.sms.tencent.sign` | 短信签名 (控制台已审核) |
| `jc-rag.sms.tencent.template-code` | 模板 ID (变量 {1}=验证码 {2}=有效分钟) |

> 渠道凭证属于环境相关配置, 按分层约定写入 `application-{env}.yml` (application.yml 保留注释模板), 禁止硬编码。

## Redis 键设计

| Key | TTL | 用途 |
|-----|-----|------|
| `jc:sms:limit:{phone}` | `send-interval-seconds` | 发送限流 (`setIfAbsent` 原子占位, 存在即限流中) |
| `jc:sms:code:{phone}` | `code-ttl-seconds` | 验证码明文 (默认 4 位纯数字, 长度/字符类型枚举化), 校验成功后立即删除 |

- 渠道发送失败时删除限流 key, 允许用户立即重试, 不被空转占位惩罚。
- `SmsCodeService#getRemainingInterval(phone)` 只读限流 key 剩余 TTL, 供前端同步倒计时。

## 验证码生成 (枚举重载)

`SmsCodeService#sendCode` 提供三个重载, 长度与字符类型只允许枚举传参:

| 重载 | 说明 |
|------|------|
| `sendCode(phone)` | 默认: `SmsCodeLength.FOUR` + `SmsCodeType.NUMERIC` (4 位纯数字) |
| `sendCode(phone, length)` | 指定长度 (FOUR/FIVE/SIX), 类型默认纯数字 |
| `sendCode(phone, length, type)` | 指定长度 + 字符类型 (NUMERIC 纯数字 / ALPHANUMERIC 数字+字母) |

字母数字码全大写生成, 字符集剔除易混淆字符 (0/O/1/I/L); 校验时忽略大小写比对。

## 接口 (均在 `jc-rag-user:8082`, `SmsAuthController`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/sms/send` | 发送验证码。时序: 滑动验证 token peek 预检 → 限流预检 (返回剩余秒数, 不消费 token) → consumeToken → 发送。响应 `SmsSendResultVO`: `sent=true` 成功发送且回执携带 `codeLength` (验证码位数, 前端据此判断输入到第几位自动登录); `sent=false` + `waitSeconds` 限流中 |
| POST | `/api/auth/sms/login` | 验证码登录。校验通过后按 `phone` 查用户, 不存在时: 注册开关开放则自动注册 (username=手机号, 随机密码哈希——只能通过短信登录) 并返回 `message="已自动注册并登录"`; 注册开关关闭则返回 `REGISTER_CLOSED` 失败, 提示"当前手机号尚未注册，请联系管理员" |

滑动验证 token 为一次性凭证 (`CaptchaService#consumeToken`), 发送成功后前端保持滑块通过态, 倒计时归零后重置要求重新滑动; `peekToken` 用于预检不消费, 避免限流中白白烧掉 token。

## 前端流程 (user-web `LoginView.vue` 验证码 tab)

1. 手机号实时格式校验 (`/^1[3-9]\d{9}$/`), 非法时输入框红色 + 滑块被遮罩拦截 (提示"请输入正确的手机号")
2. 滑块通过后验证码输入区平滑展开, 首次自动调用 `/api/auth/sms/send`
3. 倒计时存 localStorage (`jc_rag_sms_cd_{phone}` 存 deadline 时间戳), 页面刷新不丢; 命中后端限流时用 `waitSeconds` 同步, 保证前后端一致
4. 按钮"登录 / 注册", 未注册手机号由后端自动注册, 登录前轮询 `/auth/me` 确认 session 生效再跳转
5. 滑块状态与短信倒计时强绑定: 发送成功后保持"验证通过"态不重置; 重置时机仅限倒计时归零 / 发送失败 / 登录成功, tab 切换不重置; 刷新/手机号切换后仅当倒计时仍在进行才恢复滑块通过态, 否则清除缓存要求重新滑动
6. 退出登录 (`ChatHomeView` logout) 调 `POST /api/auth/logout` 销毁服务端会话, 并统一清理登录页本地持久化状态 (`utils/login-state.ts` 的 `clearLoginLocalState`), 保证退出后不能免登录访问鉴权页面

## 安全约束

- `test-enabled` 后门命中时仅 `log.warn` 放行, 不落审计以外的痕迹; **生产环境必须保持 false**, 配置评审时作为检查项
- 验证码登录不要求密码, 安全性依赖: 滑动验证 (人机) + 发送限流 (频控) + 验证码短 TTL + 一次性消费
- 自动注册用户的 `password_hash` 为随机 BCrypt 值, 无法通过账号密码登录, 不存在弱密码面
