package com.jc.rag.admin.config;

import com.jc.rag.admin.config.props.StorageProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioClientConfig {

    @Bean
    public MinioClient minioClient(StorageProperties storageProperties) {
        return MinioClient.builder()
            .endpoint(storageProperties.getMinio().getEndpoint())
            .credentials(
                storageProperties.getMinio().getAccessKey(),
                storageProperties.getMinio().getSecretKey()
            )
            .build();
    }
}
