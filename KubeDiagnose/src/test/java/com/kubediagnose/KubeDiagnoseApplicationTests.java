package com.kubediagnose;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Application context test for KubeDiagnose.
 * Uses test properties to avoid requiring actual Kubernetes cluster.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "kubernetes.kubeconfig-path="  // Disable kubeconfig loading for tests
})
class KubeDiagnoseApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring context loads successfully
    }
}
