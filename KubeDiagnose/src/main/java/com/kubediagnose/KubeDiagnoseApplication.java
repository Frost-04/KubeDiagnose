package com.kubediagnose;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for KubeDiagnose.
 * A diagnostic tool for debugging Kubernetes Pods and Services.
 */
@SpringBootApplication
public class KubeDiagnoseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KubeDiagnoseApplication.class, args);
    }
}
