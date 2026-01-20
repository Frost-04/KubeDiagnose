package com.kubediagnose.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/** Bulk pod diagnostics for a namespace. */
@JsonPropertyOrder({
    "summary", "namespace", "totalPods", "criticalCount", "warningCount", "healthyCount", "results"
})
public class BulkPodDiagnosticResult {

    private Summary summary;
    private String namespace;
    private int totalPods;
    private int criticalCount;
    private int warningCount;
    private int healthyCount;
    private List<PodDiagnosticResult> results;

    public BulkPodDiagnosticResult() {
    }

    // getters/setters
    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getTotalPods() {
        return totalPods;
    }

    public void setTotalPods(int totalPods) {
        this.totalPods = totalPods;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getHealthyCount() {
        return healthyCount;
    }

    public void setHealthyCount(int healthyCount) {
        this.healthyCount = healthyCount;
    }

    public List<PodDiagnosticResult> getResults() {
        return results;
    }

    public void setResults(List<PodDiagnosticResult> results) {
        this.results = results;
    }

    /** High-level summary for bulk pod checks. */
    @JsonPropertyOrder({"diagnosticTime", "resourceType", "overallHealth", "message"})
    public static class Summary {
        private String diagnosticTime;
        private String resourceType;
        private String overallHealth;
        private String message;

        public Summary() {
            this.resourceType = "Pods (Bulk)";
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

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
