package com.kubediagnose.analyzer;

import com.kubediagnose.model.PodDiagnosticResult;
import com.kubediagnose.rules.PodDiagnosticRules;
import io.kubernetes.client.openapi.models.V1Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Pod diagnostics analyzer. */
@Component
public class PodAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(PodAnalyzer.class);

    /** Analyze a pod across all rules. */
    public PodDiagnosticResult analyze(V1Pod pod) {
        logger.debug("Analyzing pod: {}/{}",
                     pod.getMetadata().getNamespace(),
                     pod.getMetadata().getName());

        PodDiagnosticResult result = new PodDiagnosticResult();
        List<String> causes = new ArrayList<>();
        List<String> evidence = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        result.setResourceName(pod.getMetadata().getName());
        result.setNamespace(pod.getMetadata().getNamespace());
        result.setPhase(pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown");

        PodDiagnosticRules.checkCrashLoopBackOff(pod, causes, evidence, actions);
        PodDiagnosticRules.checkImagePullErrors(pod, causes, evidence, actions);
        PodDiagnosticRules.checkOOMKilled(pod, causes, evidence, actions);
        PodDiagnosticRules.checkProbeFailures(pod, causes, evidence, actions);

        int totalRestarts = PodDiagnosticRules.checkHighRestartCount(pod, causes, evidence, actions);
        result.setRestartCount(totalRestarts);

        result.setContainerStatuses(PodDiagnosticRules.buildContainerStatuses(pod));

        String status = determineOverallStatus(pod, causes);
        result.setStatus(status);

        if (causes.isEmpty()) {
            causes.add("No issues detected");
            evidence.add("Pod phase: " + result.getPhase());
            evidence.add("All containers appear healthy");
            actions.add("No action required - pod appears to be running normally");
        }

        result.setProbableCauses(causes);
        result.setEvidence(evidence);
        result.setSuggestedActions(actions);

        PodDiagnosticResult.Summary summary = buildSummary(result, causes);
        result.setSummary(summary);

        logger.debug("Pod analysis complete. Found {} issues",
                     causes.size() == 1 && causes.contains("No issues detected") ? 0 : causes.size());

        return result;
    }

    /** Build the summary section. */
    private PodDiagnosticResult.Summary buildSummary(PodDiagnosticResult result, List<String> causes) {
        PodDiagnosticResult.Summary summary = new PodDiagnosticResult.Summary();
        summary.setOverallHealth(result.getStatus());

        boolean noIssues = causes.size() == 1 && causes.contains("No issues detected");
        int issueCount = noIssues ? 0 : causes.size();
        summary.setIssueCount(issueCount);

        String message;
        if (noIssues) {
            message = String.format("Pod '%s' is healthy and running normally.", result.getResourceName());
        } else {
            String issueWord = issueCount == 1 ? "issue" : "issues";
            message = String.format("Pod '%s' has %d %s requiring attention. Status: %s",
                                    result.getResourceName(), issueCount, issueWord, result.getStatus());
        }
        summary.setMessage(message);

        return summary;
    }

    /** Determine overall status from phase and causes. */
    private String determineOverallStatus(V1Pod pod, List<String> causes) {
        if (pod.getStatus() == null) {
            return "Unknown";
        }

        String phase = pod.getStatus().getPhase();

        for (String cause : causes) {
            if (cause.contains("CrashLoopBackOff") ||
                cause.contains("OOMKilled") ||
                cause.contains("ImagePullBackOff") ||
                cause.contains("ErrImagePull")) {
                return "Critical";
            }
        }

        if (!causes.isEmpty()) {
            return "Warning";
        }

        if ("Running".equals(phase)) {
            if (pod.getStatus().getContainerStatuses() != null) {
                boolean allReady = pod.getStatus().getContainerStatuses().stream()
                        .allMatch(cs -> cs.getReady() != null && cs.getReady());
                if (allReady) {
                    return "Healthy";
                } else {
                    return "Warning";
                }
            }
            return "Healthy";
        } else if ("Pending".equals(phase)) {
            return "Warning";
        } else if ("Succeeded".equals(phase)) {
            return "Completed";
        } else if ("Failed".equals(phase)) {
            return "Critical";
        }

        return "Unknown";
    }
}
