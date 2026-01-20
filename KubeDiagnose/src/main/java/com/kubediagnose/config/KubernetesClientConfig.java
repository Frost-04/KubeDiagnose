package com.kubediagnose.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileReader;
import java.io.IOException;

/** Kubernetes API client configuration. */
@Configuration
public class KubernetesClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Value("${kubernetes.kubeconfig-path:#{null}}")
    private String kubeconfigPath;

    /** Create ApiClient: try kubeconfig, then fall back to in-cluster. */
    @Bean
    public ApiClient apiClient() throws IOException {
        ApiClient client;

        if (kubeconfigPath != null && !kubeconfigPath.isEmpty()) {
            String expandedPath = kubeconfigPath.replace("${user.home}", System.getProperty("user.home"));
            try {
                logger.info("Loading kubeconfig from: {}", expandedPath);
                KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new FileReader(expandedPath));
                client = ClientBuilder.kubeconfig(kubeConfig).build();
                logger.info("Successfully loaded kubeconfig for local cluster access");
            } catch (IOException e) {
                logger.warn("Failed to load kubeconfig from {}, attempting in-cluster config", expandedPath);
                client = ClientBuilder.cluster().build();
            }
        } else {
            logger.info("No kubeconfig path specified, using in-cluster configuration");
            client = ClientBuilder.cluster().build();
        }

        LenientJSON.configure();

        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);

        return client;
    }

    /** CoreV1Api bean for core resources. */
    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }
}
