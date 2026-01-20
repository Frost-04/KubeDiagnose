package com.kubediagnose.controller;

import com.kubediagnose.model.BulkServiceDiagnosticResult;
import com.kubediagnose.model.ErrorResponse;
import com.kubediagnose.model.ServiceDiagnosticResult;
import com.kubediagnose.service.ServiceDebugService;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Service debug APIs. */
@RestController
@RequestMapping("/api/debug")
public class ServiceDebugController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDebugController.class);

    private final ServiceDebugService serviceDebugService;

    public ServiceDebugController(ServiceDebugService serviceDebugService) {
        this.serviceDebugService = serviceDebugService;
    }

    /** Debug a single service. */
    @GetMapping("/service/{namespace}/{serviceName}")
    public ResponseEntity<?> debugService(
            @PathVariable String namespace,
            @PathVariable String serviceName) {

        logger.info("Received debug request for service: {}/{}", namespace, serviceName);

        try {
            ServiceDiagnosticResult result = serviceDebugService.debugService(namespace, serviceName);
            return ResponseEntity.ok(result);

        } catch (ApiException e) {
            logger.error("API error while debugging service {}/{}: {} - {}",
                        namespace, serviceName, e.getCode(), e.getMessage());

            HttpStatus status = mapApiExceptionToHttpStatus(e);
            ErrorResponse error = new ErrorResponse(
                    status.getReasonPhrase(),
                    buildServiceErrorMessage(e, namespace, serviceName),
                    status.value()
            );
            return ResponseEntity.status(status).body(error);

        } catch (Exception e) {
            logger.error("Unexpected error while debugging service {}/{}: {}",
                        namespace, serviceName, e.getMessage(), e);

            ErrorResponse error = new ErrorResponse(
                    "Internal Server Error",
                    "An unexpected error occurred while debugging the service: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /** Debug all services in a namespace. */
    @GetMapping("/services/{namespace}")
    public ResponseEntity<?> debugAllServices(@PathVariable String namespace) {

        logger.info("Received bulk debug request for all services in namespace: {}", namespace);

        try {
            BulkServiceDiagnosticResult result = serviceDebugService.debugAllServices(namespace);
            return ResponseEntity.ok(result);

        } catch (ApiException e) {
            logger.error("API error while debugging services in namespace {}: {} - {}",
                        namespace, e.getCode(), e.getMessage());

            HttpStatus status = mapApiExceptionToHttpStatus(e);
            ErrorResponse error = new ErrorResponse(
                    status.getReasonPhrase(),
                    buildNamespaceErrorMessage(e, namespace, "services"),
                    status.value()
            );
            return ResponseEntity.status(status).body(error);

        } catch (Exception e) {
            logger.error("Unexpected error while debugging services in namespace {}: {}",
                        namespace, e.getMessage(), e);

            ErrorResponse error = new ErrorResponse(
                    "Internal Server Error",
                    "An unexpected error occurred while debugging services: " + e.getMessage(),
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

    /** Friendly error message for service errors. */
    private String buildServiceErrorMessage(ApiException e, String namespace, String serviceName) {
        return switch (e.getCode()) {
            case 404 -> String.format("Service '%s' not found in namespace '%s'", serviceName, namespace);
            case 401 -> "Authentication failed. Check your kubeconfig credentials.";
            case 403 -> String.format("Access denied to service '%s' in namespace '%s'. Check RBAC permissions.",
                                      serviceName, namespace);
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
