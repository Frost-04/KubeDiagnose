package com.kubediagnose.service;

import com.kubediagnose.analyzer.ServiceAnalyzer;
import com.kubediagnose.model.BulkServiceDiagnosticResult;
import com.kubediagnose.model.ServiceDiagnosticResult;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service class for Kubernetes service debugging operations.
 * Fetches service and related data from Kubernetes API and delegates analysis to ServiceAnalyzer.
 */
@Service
public class ServiceDebugService {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDebugService.class);

    // Constants for CoreDNS detection
    private static final String KUBE_SYSTEM_NAMESPACE = "kube-system";
    private static final String COREDNS_LABEL_SELECTOR = "k8s-app=kube-dns";

    private final CoreV1Api coreV1Api;
    private final ServiceAnalyzer serviceAnalyzer;

    public ServiceDebugService(CoreV1Api coreV1Api, ServiceAnalyzer serviceAnalyzer) {
        this.coreV1Api = coreV1Api;
        this.serviceAnalyzer = serviceAnalyzer;
    }

    /**
     * Debugs a specific service by fetching its details and running diagnostic analysis.
     *
     * @param namespace The namespace where the service is located
     * @param serviceName The name of the service to debug
     * @return ServiceDiagnosticResult containing diagnostic information
     * @throws ApiException if there's an error communicating with Kubernetes API
     */
    public ServiceDiagnosticResult debugService(String namespace, String serviceName) throws ApiException {
        logger.info("Starting debug for service: {}/{}", namespace, serviceName);

        // Fetch the service from Kubernetes API
        V1Service service = coreV1Api.readNamespacedService(serviceName, namespace)
                .execute();

        if (service == null) {
            throw new ApiException(404, "Service not found: " + namespace + "/" + serviceName);
        }

        logger.debug("Successfully fetched service: {}/{}", namespace, serviceName);

        // Fetch endpoints for the service
        V1Endpoints endpoints = fetchEndpoints(namespace, serviceName);

        // Fetch all pods in the namespace for selector matching analysis
        List<V1Pod> podsInNamespace = fetchPodsInNamespace(namespace);

        // Fetch CoreDNS pods to check DNS availability
        List<V1Pod> coreDnsPods = fetchCoreDnsPods();

        // Analyze the service using the analyzer
        ServiceDiagnosticResult result = serviceAnalyzer.analyze(
                service, endpoints, podsInNamespace, coreDnsPods);

        logger.info("Debug complete for service: {}/{}. Status: {}",
                    namespace, serviceName, result.getStatus());

        return result;
    }

    /**
     * Debugs all services in a namespace by fetching all services and running diagnostic analysis on each.
     * Results are sorted by severity: Critical → Warning → Healthy.
     * Does not fail the entire request if analysis of one service fails.
     *
     * @param namespace The namespace to debug all services in
     * @return BulkServiceDiagnosticResult containing diagnostic results for all services
     * @throws ApiException if there's an error fetching the service list from Kubernetes API
     */
    public BulkServiceDiagnosticResult debugAllServices(String namespace) throws ApiException {
        logger.info("Starting bulk debug for all services in namespace: {}", namespace);

        // Fetch all services in the namespace
        V1ServiceList serviceList = coreV1Api.listNamespacedService(namespace).execute();

        List<V1Service> services = serviceList.getItems() != null ? serviceList.getItems() : new ArrayList<>();
        logger.debug("Found {} services in namespace: {}", services.size(), namespace);

        // Pre-fetch shared data to avoid redundant API calls
        List<V1Pod> podsInNamespace = fetchPodsInNamespace(namespace);
        List<V1Pod> coreDnsPods = fetchCoreDnsPods();

        List<ServiceDiagnosticResult> results = new ArrayList<>();
        int criticalCount = 0;
        int warningCount = 0;
        int healthyCount = 0;

        // Analyze each service, handling failures gracefully
        for (V1Service service : services) {
            try {
                String serviceName = service.getMetadata() != null ? service.getMetadata().getName() : "unknown";
                logger.debug("Analyzing service: {}", serviceName);

                // Fetch endpoints for this specific service
                V1Endpoints endpoints = fetchEndpoints(namespace, serviceName);

                // Analyze using the same logic as single service debug
                ServiceDiagnosticResult result = serviceAnalyzer.analyze(
                        service, endpoints, podsInNamespace, coreDnsPods);
                results.add(result);

                // Count by severity
                switch (result.getStatus()) {
                    case "Critical" -> criticalCount++;
                    case "Warning" -> warningCount++;
                    case "Healthy" -> healthyCount++;
                    default -> warningCount++; // Unknown statuses count as warnings
                }
            } catch (Exception e) {
                String serviceName = service.getMetadata() != null ? service.getMetadata().getName() : "unknown";
                logger.warn("Failed to analyze service {}: {}", serviceName, e.getMessage());

                // Create an error result for the failed service
                ServiceDiagnosticResult errorResult = createErrorResult(service, e);
                results.add(errorResult);
                criticalCount++;
            }
        }

        // Sort results by severity: Critical → Warning → Healthy
        results.sort(Comparator.comparingInt(this::getSeverityOrder));

        // Build the bulk result
        BulkServiceDiagnosticResult bulkResult = new BulkServiceDiagnosticResult();
        bulkResult.setNamespace(namespace);
        bulkResult.setTotalServices(services.size());
        bulkResult.setCriticalCount(criticalCount);
        bulkResult.setWarningCount(warningCount);
        bulkResult.setHealthyCount(healthyCount);
        bulkResult.setResults(results);

        // Build summary
        BulkServiceDiagnosticResult.Summary summary = buildBulkSummary(namespace, services.size(),
                                                                        criticalCount, warningCount, healthyCount);
        bulkResult.setSummary(summary);

        logger.info("Bulk debug complete for namespace: {}. Total: {}, Critical: {}, Warning: {}, Healthy: {}",
                    namespace, services.size(), criticalCount, warningCount, healthyCount);

        return bulkResult;
    }

    /**
     * Returns a severity order for sorting (lower = more severe).
     */
    private int getSeverityOrder(ServiceDiagnosticResult result) {
        return switch (result.getStatus()) {
            case "Critical" -> 0;
            case "Warning" -> 1;
            case "Healthy" -> 2;
            default -> 1; // Unknown treated as warning
        };
    }

    /**
     * Creates an error result for a service that failed analysis.
     */
    private ServiceDiagnosticResult createErrorResult(V1Service service, Exception e) {
        ServiceDiagnosticResult result = new ServiceDiagnosticResult();
        result.setResourceName(service.getMetadata() != null ? service.getMetadata().getName() : "unknown");
        result.setNamespace(service.getMetadata() != null ? service.getMetadata().getNamespace() : "unknown");
        result.setStatus("Critical");
        result.setServiceType(service.getSpec() != null ? service.getSpec().getType() : "Unknown");
        result.setSelector(service.getSpec() != null ? service.getSpec().getSelector() : null);
        result.setProbableCauses(List.of("Failed to analyze service: " + e.getMessage()));
        result.setEvidence(List.of("Analysis error occurred"));
        result.setSuggestedActions(List.of("Check service manually using kubectl describe service " + result.getResourceName()));
        result.setPorts(new ArrayList<>());
        result.setCoreDnsExists(true);

        // Build summary for error result
        ServiceDiagnosticResult.Summary summary = new ServiceDiagnosticResult.Summary();
        summary.setOverallHealth("Critical");
        summary.setIssueCount(1);
        summary.setMessage("Failed to analyze service: " + e.getMessage());
        result.setSummary(summary);

        return result;
    }

    /**
     * Builds summary for bulk service diagnostic result.
     */
    private BulkServiceDiagnosticResult.Summary buildBulkSummary(String namespace, int total,
                                                                  int critical, int warning, int healthy) {
        BulkServiceDiagnosticResult.Summary summary = new BulkServiceDiagnosticResult.Summary();

        // Determine overall health
        String overallHealth;
        if (critical > 0) {
            overallHealth = "Critical";
        } else if (warning > 0) {
            overallHealth = "Warning";
        } else {
            overallHealth = "Healthy";
        }
        summary.setOverallHealth(overallHealth);

        // Build message
        String message;
        if (total == 0) {
            message = String.format("No services found in namespace '%s'.", namespace);
        } else if (critical == 0 && warning == 0) {
            message = String.format("All %d services in namespace '%s' are healthy.", total, namespace);
        } else {
            message = String.format("Namespace '%s': %d services analyzed - %d critical, %d warning, %d healthy.",
                                    namespace, total, critical, warning, healthy);
        }
        summary.setMessage(message);

        return summary;
    }

    /**
     * Fetches endpoints for a service.
     * Endpoints share the same name as the service.
     *
     * @param namespace The namespace
     * @param serviceName The service name
     * @return V1Endpoints or null if not found
     */
    private V1Endpoints fetchEndpoints(String namespace, String serviceName) {
        try {
            return coreV1Api.readNamespacedEndpoints(serviceName, namespace)
                    .execute();
        } catch (ApiException e) {
            logger.warn("Could not fetch endpoints for service {}/{}: {}",
                        namespace, serviceName, e.getMessage());
            return null;
        }
    }

    /**
     * Fetches all pods in a namespace.
     *
     * @param namespace The namespace to fetch pods from
     * @return List of pods in the namespace
     */
    private List<V1Pod> fetchPodsInNamespace(String namespace) {
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(namespace)
                    .execute();
            return podList.getItems() != null ? podList.getItems() : new ArrayList<>();
        } catch (ApiException e) {
            logger.warn("Could not fetch pods in namespace {}: {}", namespace, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetches CoreDNS pods from kube-system namespace.
     * CoreDNS pods typically have the label 'k8s-app=kube-dns'.
     *
     * @return List of CoreDNS pods
     */
    private List<V1Pod> fetchCoreDnsPods() {
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(KUBE_SYSTEM_NAMESPACE)
                    .labelSelector(COREDNS_LABEL_SELECTOR)
                    .execute();
            return podList.getItems() != null ? podList.getItems() : new ArrayList<>();
        } catch (ApiException e) {
            logger.warn("Could not fetch CoreDNS pods: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
