package com.kubediagnose.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * DTO representing the bulk diagnostic result for all Services in a namespace.
 * Contains aggregated results from analyzing multiple services.
 */
@JsonPropertyOrder({
    "summary", "namespace", "totalServices", "criticalCount", "warningCount", "healthyCount", "results"
})
public class BulkServiceDiagnosticResult {

    private Summary summary;
    private String namespace;
    private int totalServices;
    private int criticalCount;
    private int warningCount;
    private int healthyCount;
    private List<ServiceDiagnosticResult> results;

    public BulkServiceDiagnosticResult() {
    }

    // Getters and Setters

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

    public int getTotalServices() {
        return totalServices;
    }

    public void setTotalServices(int totalServices) {
        this.totalServices = totalServices;
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

    public List<ServiceDiagnosticResult> getResults() {
        return results;
    }

    public void setResults(List<ServiceDiagnosticResult> results) {
        this.results = results;
    }

    /**
     * Summary section providing a quick overview of the bulk diagnosis.
     */
    @JsonPropertyOrder({"diagnosticTime", "resourceType", "overallHealth", "message"})
    public static class Summary {
        private String diagnosticTime;
        private String resourceType;
        private String overallHealth;
        private String message;

        public Summary() {
            this.resourceType = "Services (Bulk)";
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
