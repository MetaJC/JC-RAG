/*
 * Copyright 2024-2026 OpenJC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 本文件来源于 JC RAG Community Edition
 * 开源地址: https://gitee.com/Open-JC/jc-rag | https://github.com/MetaJC/JC-RAG
 * 二次开发请保留此出处声明。
 */


package com.jc.rag.common.banner;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

/**
 * JC RAG 启动 banner 抽象基类.
 * <p>在 Spring 容器就绪后输出 ASCII 图案 + 启动信息 (服务耗时 / Profile / 端口 / 访问地址等).</p>
 * <p>子类仅负责构建图案和版本/版本类型名称; 所有通用逻辑 (耗时计算 / 居中 / 拼装) 集中在本类.</p>
 * <p>代码隔离: Pro 版实现在 {@code jc-rag-pro-core} 内, 不污染 community 源码.</p>
 *
 * <p>布局: 短信息 (Profile / Port / Version / Edition / Cost) 放图案右侧 (Redis 风格),
 * 长信息 (URL) 放图案下方, 最底部 2 行 Gitee + Github 占位.</p>
 *
 * <p>触发时机: {@link ApplicationReadyEvent} (启动完全成功, 服务可接收请求之后).
 * 相比 CommandLineRunner, ApplicationReadyEvent 是 Spring Boot 启动链路的最终事件,
 * 在此之后才打印 banner 可保证: 如果启动过程中抛错, banner 不会出现.</p>
 *
 * @author JC
 */
public abstract class JcRagStartupBanner implements ApplicationListener<ApplicationReadyEvent> {

    /** main() 入口打点 (毫秒), 用于计算服务启动总耗时. */
    private static volatile long mainStartTimeMs = 0L;

    /** 是否已经打印过 banner (防止重复触发, 例如单元测试时多 Context). */
    private static volatile boolean printed = false;

    /** ApplicationReadyEvent 触发时间 (毫秒), 作为启动完成终点. */
    private long endTimeMs = 0L;

    protected final Environment environment;

    protected JcRagStartupBanner(Environment environment) {
        this.environment = environment;
    }

    /**
     * 在各启动类 main() 第一行调用, 记录服务启动起点.
     */
    public static void markStart() {
        mainStartTimeMs = System.currentTimeMillis();
    }

    @Override
    public final void onApplicationEvent(ApplicationReadyEvent event) {
        // 启动完全成功后才打印, 启动失败 / 异常不会输出 banner
        if (printed) return;
        printed = true;
        this.endTimeMs = System.currentTimeMillis();
        try {
            long start = mainStartTimeMs == 0L ? System.currentTimeMillis() : mainStartTimeMs;
            long costMs = Math.max(0L, this.endTimeMs - start);
            String version = readVersion();
            int port = Integer.parseInt(environment.getProperty("server.port", "8080"));
            String contextPath = environment.getProperty("server.servlet.context-path", "");
            String localUrl = "http://localhost:" + port + contextPath;
            String networkIp = detectNetworkIp();
            String networkUrl = "http://" + networkIp + ":" + port + contextPath;

            List<String> bannerLines = buildBannerLines(version);
            int bannerHeight = bannerLines.size();
            List<String> sideInfo = Arrays.asList(
                "+ Service   started in " + formatSeconds(costMs) + "s",
                "+ Local     " + localUrl,
                "+ Network   " + networkUrl,
                "+ Health    " + localUrl + "/api/health/basic",
                "+ API Base  " + localUrl + "/api"
            );

            int bannerWidth = bannerLines.stream().mapToInt(String::length).max().orElse(0);
            int sideWidth = sideInfo.stream().mapToInt(String::length).max().orElse(0);

            List<String> out = new ArrayList<>();
            out.add(""); // 图案上方空一行
            for (int i = 0; i < bannerHeight; i++) {
                String left = bannerLines.get(i);
                String right = i < sideInfo.size() ? "  " + sideInfo.get(i) : "";
                out.add(padRight(left, bannerWidth) + right);
            }
            out.add("");
            out.add("  " + getEditionPrefix() + " v" + version + "    [" + getEditionSuffix() + "]");
            out.addAll(buildProjectHomeLines());

            int contentWidth = out.stream().mapToInt(String::length).max().orElse(0);
            int terminalWidth = detectTerminalWidth();
            int padding = Math.max(0, (terminalWidth - contentWidth) / 2);
            String prefix = " ".repeat(padding);
            for (String line : out) {
                System.out.println(prefix + line);
            }
        } catch (Exception ex) {
            // 启动 banner 失败不影响服务运行
            ex.printStackTrace(System.err);
        }
    }

    /** 图案行. 子类负责绘制. */
    protected abstract List<String> buildBannerLines(String version);

    /** 版本行前缀, 例如 {@code "JC RAG"} / {@code "JC RAG PRO"}. */
    protected abstract String getEditionPrefix();

    /** 版本行后缀 (Edition 标识), 例如 {@code "Community Edition"} / {@code "Professional Edition"}. */
    protected abstract String getEditionSuffix();

    /**
     * 读取应用版本号. 顺序: MANIFEST (spring.application.version) → yml (jc-rag.banner.version) → 默认值.
     */
    private String readVersion() {
        String v = environment.getProperty("spring.application.version");
        if (v != null && !v.isEmpty() && !"unknown".equals(v)) {
            return v;
        }
        v = environment.getProperty("jc-rag.banner.version");
        if (v != null && !v.isEmpty()) {
            return v;
        }
        return "1.0.0-SNAPSHOT";
    }

    private List<String> buildProjectHomeLines() {
        return Arrays.asList(
            "  JC RAG Project Home:",
            "    + Gitee    https://gitee.com/jc-rag/jc-rag (placeholder)",
            "    + Github   https://github.com/jc-rag/jc-rag (placeholder)",
            ""
        );
    }

    private String padRight(String s, int n) {
        return s.length() >= n ? s : s + " ".repeat(n - s.length());
    }

    private String formatSeconds(long ms) {
        return String.format("%.3f", ms / 1000.0);
    }

    private int detectTerminalWidth() {
        try {
            String columns = System.getenv("COLUMNS");
            if (columns != null && !columns.isEmpty()) {
                int width = Integer.parseInt(columns.trim());
                if (width > 0) {
                    return width;
                }
            }
        } catch (NumberFormatException ex) {
            // 环境变量非数字时使用默认值
        }
        return 100;
    }

    private String detectNetworkIp() {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            if (nis == null) {
                return "127.0.0.1";
            }
            while (nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (ni.isLoopback() || ni.isVirtual() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                        continue;
                    }
                    String host = addr.getHostAddress();
                    if (host != null && !host.contains(":")) {
                        return host;
                    }
                }
            }
        } catch (Exception ex) {
            // IP 探测失败时使用 localhost
        }
        return "127.0.0.1";
    }
}
