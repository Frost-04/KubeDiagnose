package com.kubediagnose.controller;

import com.kubediagnose.model.ErrorResponse;
import com.kubediagnose.model.NamespaceListResponse;
import com.kubediagnose.service.NamespaceService;
import io.kubernetes.client.openapi.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Namespace discovery APIs. */
@RestController
@RequestMapping("/api")
public class NamespaceController {

    private static final Logger logger = LoggerFactory.getLogger(NamespaceController.class);

    private final NamespaceService namespaceService;

    public NamespaceController(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    /** List all namespaces. */
    @GetMapping("/namespaces")
    public ResponseEntity<?> listNamespaces() {

        logger.info("Received request to list all namespaces");

        try {
            NamespaceListResponse result = namespaceService.listNamespaces();
            return ResponseEntity.ok(result);

        } catch (ApiException e) {
            logger.error("API error while listing namespaces: {} - {}", e.getCode(), e.getMessage());

            HttpStatus status = mapApiExceptionToHttpStatus(e);
            ErrorResponse error = new ErrorResponse(
                    status.getReasonPhrase(),
                    buildErrorMessage(e),
                    status.value()
            );
            return ResponseEntity.status(status).body(error);

        } catch (Exception e) {
            logger.error("Unexpected error while listing namespaces: {}", e.getMessage(), e);

            ErrorResponse error = new ErrorResponse(
                    "Internal Server Error",
                    "An unexpected error occurred while listing namespaces: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /** Map ApiException to status. */
    private HttpStatus mapApiExceptionToHttpStatus(ApiException e) {
        return switch (e.getCode()) {
            case 401, 403 -> HttpStatus.FORBIDDEN;
            case 400 -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /** Friendly error message. */
    private String buildErrorMessage(ApiException e) {
        return switch (e.getCode()) {
            case 401 -> "Authentication failed. Check your kubeconfig credentials.";
            case 403 -> "Access denied to list namespaces. Check RBAC permissions.";
            default -> "Error communicating with Kubernetes API: " + e.getMessage();
        };
    }
}
