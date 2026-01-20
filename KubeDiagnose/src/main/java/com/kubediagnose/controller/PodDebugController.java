package com.kubediagnose.controller;

import com.kubediagnose.model.BulkPodDiagnosticResult;
import com.kubediagnose.model.ErrorResponse;
import com.kubediagnose.model.PodDiagnosticResult;
import com.kubediagnose.service.PodDebugService;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Pod debug APIs. */
@RestController
@RequestMapping("/api/debug")
public class PodDebugController {

    private static final Logger logger = LoggerFactory.getLogger(PodDebugController.class);

    private final PodDebugService podDebugService;

    public PodDebugController(PodDebugService podDebugService) {
        this.podDebugService = podDebugService;
    }

    /** Debug a single pod. */
    @GetMapping("/pod/{namespace}/{podName}")
    public ResponseEntity<?> debugPod(
            @PathVariable String namespace,
            @PathVariable String podName) {

        logger.info("Received debug request for pod: {}/{}", namespace, podName);

        try {
            PodDiagnosticResult result = podDebugService.debugPod(namespace, podName);
            return ResponseEntity.ok(result);

        } catch (ApiException e) {
            logger.error("API error while debugging pod {}/{}: {} - {}",
                        namespace, podName, e.getCode(), e.getMessage());

            HttpStatus status = mapApiExceptionToHttpStatus(e);
            ErrorResponse error = new ErrorResponse(
                    status.getReasonPhrase(),
                    buildPodErrorMessage(e, namespace, podName),
                    status.value()
            );
            return ResponseEntity.status(status).body(error);

        } catch (Exception e) {
            logger.error("Unexpected error while debugging pod {}/{}: {}",
                        namespace, podName, e.getMessage(), e);

            ErrorResponse error = new ErrorResponse(
                    "Internal Server Error",
                    "An unexpected error occurred while debugging the pod: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /** Debug all pods in a namespace. */
    @GetMapping("/pods/{namespace}")
    public ResponseEntity<?> debugAllPods(@PathVariable String namespace) {

        logger.info("Received bulk debug request for all pods in namespace: {}", namespace);

        try {
            BulkPodDiagnosticResult result = podDebugService.debugAllPods(namespace);
            return ResponseEntity.ok(result);

        } catch (ApiException e) {
            logger.error("API error while debugging pods in namespace {}: {} - {}",
                        namespace, e.getCode(), e.getMessage());

            HttpStatus status = mapApiExceptionToHttpStatus(e);
            ErrorResponse error = new ErrorResponse(
                    status.getReasonPhrase(),
                    buildNamespaceErrorMessage(e, namespace, "pods"),
                    status.value()
            );
            return ResponseEntity.status(status).body(error);

        } catch (Exception e) {
            logger.error("Unexpected error while debugging pods in namespace {}: {}",
                        namespace, e.getMessage(), e);

            ErrorResponse error = new ErrorResponse(
                    "Internal Server Error",
                    "An unexpected error occurred while debugging pods: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /** Map ApiException to HTTP status. */
    private HttpStatus mapApiExceptionToHttpStatus(ApiException e) {
        return switch (e.getCode()) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 401, 403 -> HttpStatus.FORBIDDEN;
            case 400 -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /** Friendly error message for pod errors. */
    private String buildPodErrorMessage(ApiException e, String namespace, String podName) {
        return switch (e.getCode()) {
            case 404 -> String.format("Pod '%s' not found in namespace '%s'", podName, namespace);
            case 401 -> "Authentication failed. Check your kubeconfig credentials.";
            case 403 -> String.format("Access denied to pod '%s' in namespace '%s'. Check RBAC permissions.",
                                      podName, namespace);
            default -> "Error communicating with Kubernetes API: " + e.getMessage();
        };
    }

    /** Friendly error message for namespace-level errors. */
    private String buildNamespaceErrorMessage(ApiException e, String namespace, String resourceType) {
        return switch (e.getCode()) {
            case 404 -> String.format("Namespace '%s' not found", namespace);
            case 401 -> "Authentication failed. Check your kubeconfig credentials.";
            case 403 -> String.format("Access denied to %s in namespace '%s'. Check RBAC permissions.",
                                      resourceType, namespace);
            default -> "Error communicating with Kubernetes API: " + e.getMessage();
        };
    }
}
