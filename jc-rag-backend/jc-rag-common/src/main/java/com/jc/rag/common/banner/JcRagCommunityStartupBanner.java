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
