package com.kubediagnose.config;

import io.kubernetes.client.openapi.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Ensures the Kubernetes JSON parser is initialized. */
public class LenientJSON {

    private static final Logger logger = LoggerFactory.getLogger(LenientJSON.class);

    /** Seed the JSON parser so newer Kubernetes fields parse cleanly. */
    public static void configure() {
        try {
            new JSON();
            logger.debug("Kubernetes JSON parser initialized successfully");
        } catch (Exception e) {
            logger.warn("Could not initialize Kubernetes JSON parser: {}", e.getMessage());
        }
    }
}
