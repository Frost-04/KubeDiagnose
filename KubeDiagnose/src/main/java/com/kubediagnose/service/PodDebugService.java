package com.kubediagnose.service;

import com.kubediagnose.analyzer.PodAnalyzer;
import com.kubediagnose.model.BulkPodDiagnosticResult;
import com.kubediagnose.model.PodDiagnosticResult;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pod debug service. */
@Service
public class PodDebugService {

    private static final Logger logger = LoggerFactory.getLogger(PodDebugService.class);

    private final CoreV1Api coreV1Api;
    private final PodAnalyzer podAnalyzer;

    public PodDebugService(CoreV1Api coreV1Api, PodAnalyzer podAnalyzer) {
        this.coreV1Api = coreV1Api;
        this.podAnalyzer = podAnalyzer;
    }

    /** Debug a single pod. */
    public PodDiagnosticResult debugPod(String namespace, String podName) throws ApiException {
        logger.info("Starting debug for pod: {}/{}", namespace, podName);

        V1Pod pod = coreV1Api.readNamespacedPod(podName, namespace)
                .execute();

        if (pod == null) {
            throw new ApiException(404, "Pod not found: " + namespace + "/" + podName);
        }

        logger.debug("Successfully fetched pod: {}/{}", namespace, podName);

        PodDiagnosticResult result = podAnalyzer.analyze(pod);

        logger.info("Debug complete for pod: {}/{}. Status: {}",
                    namespace, podName, result.getStatus());

        return result;
    }

    /** Debug all pods in a namespace. */
    public BulkPodDiagnosticResult debugAllPods(String namespace) throws ApiException {
        logger.info("Starting bulk debug for all pods in namespace: {}", namespace);

        V1PodList podList = coreV1Api.listNamespacedPod(namespace).execute();

        List<V1Pod> pods = podList.getItems() != null ? podList.getItems() : new ArrayList<>();
        logger.debug("Found {} pods in namespace: {}", pods.size(), namespace);

        List<PodDiagnosticResult> results = new ArrayList<>();
        int criticalCount = 0;
        int warningCount = 0;
        int healthyCount = 0;

        for (V1Pod pod : pods) {
            try {
                String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown";
                logger.debug("Analyzing pod: {}", podName);

                PodDiagnosticResult result = podAnalyzer.analyze(pod);
                results.add(result);

                switch (result.getStatus()) {
                    case "Critical" -> criticalCount++;
                    case "Warning" -> warningCount++;
                    case "Healthy", "Completed" -> healthyCount++;
                    default -> warningCount++;
                }
            } catch (Exception e) {
                String podName = pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown";
                logger.warn("Failed to analyze pod {}: {}", podName, e.getMessage());

                PodDiagnosticResult errorResult = createErrorResult(pod, e);
                results.add(errorResult);
                criticalCount++;
            }
        }

        results.sort(Comparator.comparingInt(this::getSeverityOrder));

        BulkPodDiagnosticResult bulkResult = new BulkPodDiagnosticResult();
        bulkResult.setNamespace(namespace);
        bulkResult.setTotalPods(pods.size());
        bulkResult.setCriticalCount(criticalCount);
        bulkResult.setWarningCount(warningCount);
        bulkResult.setHealthyCount(healthyCount);
        bulkResult.setResults(results);

        BulkPodDiagnosticResult.Summary summary = buildBulkSummary(namespace, pods.size(),
                                                                    criticalCount, warningCount, healthyCount);
        bulkResult.setSummary(summary);

        logger.info("Bulk debug complete for namespace: {}. Total: {}, Critical: {}, Warning: {}, Healthy: {}",
                    namespace, pods.size(), criticalCount, warningCount, healthyCount);

        return bulkResult;
    }

    /** Severity order for sorting (lower = more severe). */
    private int getSeverityOrder(PodDiagnosticResult result) {
        return switch (result.getStatus()) {
            case "Critical" -> 0;
            case "Warning" -> 1;
            case "Healthy" -> 2;
            case "Completed" -> 3;
            default -> 1;
        };
    }

    /** Error result for failed analysis. */
    private PodDiagnosticResult createErrorResult(V1Pod pod, Exception e) {
        PodDiagnosticResult result = new PodDiagnosticResult();
        result.setResourceName(pod.getMetadata() != null ? pod.getMetadata().getName() : "unknown");
        result.setNamespace(pod.getMetadata() != null ? pod.getMetadata().getNamespace() : "unknown");
        result.setStatus("Critical");
        result.setPhase("Unknown");
        result.setProbableCauses(List.of("Failed to analyze pod: " + e.getMessage()));
        result.setEvidence(List.of("Analysis error occurred"));
        result.setSuggestedActions(List.of("Check pod manually using kubectl describe pod " + result.getResourceName()));
        result.setContainerStatuses(new ArrayList<>());
        result.setRestartCount(0);

        PodDiagnosticResult.Summary summary = new PodDiagnosticResult.Summary();
        summary.setOverallHealth("Critical");
        summary.setIssueCount(1);
        summary.setMessage("Failed to analyze pod: " + e.getMessage());
        result.setSummary(summary);

        return result;
    }

    /** Summary builder for bulk result. */
    private BulkPodDiagnosticResult.Summary buildBulkSummary(String namespace, int total,
                                                             int critical, int warning, int healthy) {
        BulkPodDiagnosticResult.Summary summary = new BulkPodDiagnosticResult.Summary();

        String overallHealth;
        if (critical > 0) {
            overallHealth = "Critical";
        } else if (warning > 0) {
            overallHealth = "Warning";
        } else {
            overallHealth = "Healthy";
        }
        summary.setOverallHealth(overallHealth);

        String message;
        if (total == 0) {
            message = String.format("No pods found in namespace '%s'.", namespace);
        } else if (critical == 0 && warning == 0) {
            message = String.format("All %d pods in namespace '%s' are healthy.", total, namespace);
        } else {
            message = String.format("Namespace '%s': %d pods analyzed - %d critical, %d warning, %d healthy.",
                                    namespace, total, critical, warning, healthy);
        }
        summary.setMessage(message);

        return summary;
    }
}
