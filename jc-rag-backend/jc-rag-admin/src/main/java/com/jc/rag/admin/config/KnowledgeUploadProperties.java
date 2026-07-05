package com.jc.rag.admin.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "knowledge.upload")
public class KnowledgeUploadProperties {

    @NotNull
    private DataSize maxFileSize;

    @NotNull
    private DataSize maxRequestSize;

    @NotNull
    private Duration parseTimeout;

    @NotEmpty
    private List<String> allowedExtensions;

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public DataSize getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(DataSize maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public Duration getParseTimeout() {
        return parseTimeout;
    }

    public void setParseTimeout(Duration parseTimeout) {
        this.parseTimeout = parseTimeout;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }
}
