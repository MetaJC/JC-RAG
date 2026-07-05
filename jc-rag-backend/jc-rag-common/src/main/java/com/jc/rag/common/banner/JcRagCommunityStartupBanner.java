package com.jc.rag.common.banner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 社区版启动 banner 实现.
 * <p>仅在 {@code jc-rag.edition=community} (或未配置) 时注册, Pro 版由 {@code jc-rag-pro} 模块覆盖.</p>
 * <p>Pro 版内容 / 类路径均不在本文件中, 符合"商业版内容不进社区版源码"的约束.</p>
 *
 * @author JC
 */
@Component
@ConditionalOnProperty(name = "jc-rag.edition", havingValue = "community", matchIfMissing = true)
public class JcRagCommunityStartupBanner extends JcRagStartupBanner {

    public JcRagCommunityStartupBanner(Environment environment) {
        super(environment);
    }

    @Override
    protected List<String> buildBannerLines(String version) {
        return Arrays.asList(
            "   ██╗  ██████╗    ██████╗  █████╗  ██████╗ ",
            "   ██║ ██╔════╝    ██╔══██╗██╔══██╗██╔════╝ ",
            "   ██║ ██║         ██████╔╝███████║██║  ███╗",
            "   ██║ ██║         ██╔══██╗██╔══██║██║   ██║",
            "█████║ ╚██████╗    ██║  ██║██║  ██║╚██████╔╝",
            " ╚═══╝  ╚═════╝    ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ "
        );
    }

    @Override
    protected String getEditionPrefix() {
        return "JC RAG";
    }

    @Override
    protected String getEditionSuffix() {
        return "Community Edition";
    }
}
