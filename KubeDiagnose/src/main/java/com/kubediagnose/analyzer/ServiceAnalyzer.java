package com.kubediagnose.analyzer;

import com.kubediagnose.model.ServiceDiagnosticResult;
import com.kubediagnose.rules.ServiceDiagnosticRules;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Service diagnostics analyzer. */
@Component
public class ServiceAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAnalyzer.class);

    /** Analyze a service with all rules. */
    public ServiceDiagnosticResult analyze(V1Service service, V1Endpoints endpoints,
                                           List<V1Pod> podsInNamespace, List<V1Pod> coreDnsPods) {
        logger.debug("Analyzing service: {}/{}",
                     service.getMetadata().getNamespace(),
                     service.getMetadata().getName());

        ServiceDiagnosticResult result = new ServiceDiagnosticResult();
        List<String> causes = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        // Set basic service information
        result.setResourceName(service.getMetadata().getName());
        result.setNamespace(service.getMetadata().getNamespace());
        result.setServiceType(service.getSpec() != null ? service.getSpec().getType() : "Unknown");
        result.setSelector(service.getSpec() != null ? service.getSpec().getSelector() : null);

        // Build and set port information
        result.setPorts(ServiceDiagnosticRules.buildServicePorts(service));

        // Find pods that match the service selector
        List<V1Pod> matchingPods = findMatchingPods(service, podsInNamespace);

        // Rule 1: Check for selector mismatch
        boolean selectorMismatch = ServiceDiagnosticRules.checkSelectorMismatch(
                service, podsInNamespace, causes, evidence, actions);

        // Rule 2: Check for no endpoints
        ServiceDiagnosticResult.EndpointInfo endpointInfo =
                ServiceDiagnosticRules.checkNoEndpoints(endpoints, causes, evidence, actions);
        result.setEndpointInfo(endpointInfo);

        // Rule 3: Check for port mismatch (only if we have matching pods)
        if (!matchingPods.isEmpty()) {
            ServiceDiagnosticRules.checkPortMismatch(service, matchingPods, causes, evidence, actions);
        }

        // Rule 4: Check CoreDNS status
        boolean coreDnsExists = ServiceDiagnosticRules.checkCoreDnsExists(
                coreDnsPods, causes, evidence, actions);
        result.setCoreDnsExists(coreDnsExists);

        // Determine overall status based on findings
        String status = determineOverallStatus(selectorMismatch, endpointInfo, coreDnsExists, causes);
        result.setStatus(status);

        // If no issues found, add positive evidence
        if (causes.isEmpty()) {
            causes.add("No issues detected");
            evidence.add("Service type: " + result.getServiceType());
            evidence.add("Ready endpoints: " + endpointInfo.getReadyEndpoints());
            evidence.add("CoreDNS is operational");
            actions.add("No action required - service appears to be configured correctly");
        }

        result.setProbableCauses(causes);
        result.setEvidence(evidence);
        result.setSuggestedActions(actions);

        // Build summary
        ServiceDiagnosticResult.Summary summary = buildSummary(result, causes, endpointInfo);
        result.setSummary(summary);

        logger.debug("Service analysis complete. Found {} issues",
                     causes.size() == 1 && causes.contains("No issues detected") ? 0 : causes.size());

        return result;
    }

    /** Build summary for services. */
    private ServiceDiagnosticResult.Summary buildSummary(ServiceDiagnosticResult result,
                                                         List<String> causes,
                                                         ServiceDiagnosticResult.EndpointInfo endpointInfo) {
        ServiceDiagnosticResult.Summary summary = new ServiceDiagnosticResult.Summary();
        summary.setOverallHealth(result.getStatus());

        boolean noIssues = causes.size() == 1 && causes.contains("No issues detected");
        int issueCount = noIssues ? 0 : causes.size();
        summary.setIssueCount(issueCount);

        // Build summary message
        String message;
        if (noIssues) {
            message = String.format("Service '%s' is healthy with %d ready endpoint(s).",
                                    result.getResourceName(), endpointInfo.getReadyEndpoints());
        } else {
            String issueWord = issueCount == 1 ? "issue" : "issues";
            message = String.format("Service '%s' has %d %s requiring attention. Status: %s",
                                    result.getResourceName(), issueCount, issueWord, result.getStatus());
        }
        summary.setMessage(message);

        return summary;
    }

    /** Match pods to service selectors. */
    private List<V1Pod> findMatchingPods(V1Service service, List<V1Pod> pods) {
        if (service.getSpec() == null || service.getSpec().getSelector() == null) {
            return new ArrayList<>();
        }

        Map<String, String> selector = service.getSpec().getSelector();

        return pods.stream()
                .filter(pod -> {
                    if (pod.getMetadata() == null || pod.getMetadata().getLabels() == null) {
                        return false;
                    }
                    Map<String, String> podLabels = pod.getMetadata().getLabels();
                    return selector.entrySet().stream()
                            .allMatch(entry -> entry.getValue().equals(podLabels.get(entry.getKey())));
                })
                .collect(Collectors.toList());
    }

    /** Determine status from selectors/endpoints. */
    private String determineOverallStatus(boolean selectorMismatch,
                                          ServiceDiagnosticResult.EndpointInfo endpointInfo,
                                          boolean coreDnsExists, List<String> causes) {
        // Critical conditions
        if (selectorMismatch) {
            return "Critical";
        }

        if (!coreDnsExists) {
            return "Critical";
        }

        if (endpointInfo.getReadyEndpoints() == 0 && endpointInfo.getNotReadyEndpoints() == 0) {
            return "Critical";
        }

        // Warning conditions
        if (endpointInfo.getReadyEndpoints() == 0 && endpointInfo.getNotReadyEndpoints() > 0) {
            return "Warning";
        }

        // Check for any other issues
        for (String cause : causes) {
            if (cause.contains("mismatch") || cause.contains("not match")) {
                return "Warning";
            }
        }

        // If no issues found
        if (causes.isEmpty()) {
            return "Healthy";
        }

        return "Warning";
    }
}
