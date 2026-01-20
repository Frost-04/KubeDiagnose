package com.kubediagnose.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * DTO representing the diagnostic result for a Kubernetes Pod.
 * Contains all information needed to understand why a pod might be failing.
 */
@JsonPropertyOrder({
    "summary", "resourceName", "namespace", "status", "phase", "restartCount",
    "probableCauses", "evidence", "suggestedActions", "containerStatuses"
})
public class PodDiagnosticResult {

    private Summary summary;
    private String resourceName;
    private String namespace;
    private String status;
    private String phase;
    private List<String> probableCauses;
    private List<String> evidence;
    private List<String> suggestedActions;
    private List<ContainerStatus> containerStatuses;
    private int restartCount;

    public PodDiagnosticResult() {
    }

    // Getters and Setters

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getProbableCauses() {
        return probableCauses;
    }

    public void setProbableCauses(List<String> probableCauses) {
        this.probableCauses = probableCauses;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public List<String> getSuggestedActions() {
        return suggestedActions;
    }

    public void setSuggestedActions(List<String> suggestedActions) {
        this.suggestedActions = suggestedActions;
    }

    public List<ContainerStatus> getContainerStatuses() {
        return containerStatuses;
    }

    public void setContainerStatuses(List<ContainerStatus> containerStatuses) {
        this.containerStatuses = containerStatuses;
    }

    public int getRestartCount() {
        return restartCount;
    }

    public void setRestartCount(int restartCount) {
        this.restartCount = restartCount;
    }

    /**
     * Summary section providing a quick overview of the diagnosis.
     */
    @JsonPropertyOrder({"diagnosticTime", "resourceType", "overallHealth", "issueCount", "message"})
    public static class Summary {
        private String diagnosticTime;
        private String resourceType;
        private String overallHealth;
        private int issueCount;
        private String message;

        public Summary() {
            this.resourceType = "Pod";
            this.diagnosticTime = java.time.OffsetDateTime.now().toString();
        }

        public String getDiagnosticTime() {
            return diagnosticTime;
        }

        public void setDiagnosticTime(String diagnosticTime) {
            this.diagnosticTime = diagnosticTime;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getOverallHealth() {
            return overallHealth;
        }

        public void setOverallHealth(String overallHealth) {
            this.overallHealth = overallHealth;
        }

        public int getIssueCount() {
            return issueCount;
        }

        public void setIssueCount(int issueCount) {
            this.issueCount = issueCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Inner class representing the status of a container within the pod.
     */
    @JsonPropertyOrder({"name", "state", "ready", "restartCount", "reason", "message"})
    public static class ContainerStatus {
        private String name;
        private String state;
        private String reason;
        private String message;
        private int restartCount;
        private boolean ready;

        public ContainerStatus() {
        }

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getRestartCount() {
            return restartCount;
        }

        public void setRestartCount(int restartCount) {
            this.restartCount = restartCount;
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }
    }
}
