package com.kubediagnose.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the diagnostic result for a Kubernetes Service.
 * Contains all information needed to understand why a service might not be working correctly.
 */
@JsonPropertyOrder({
    "summary", "resourceName", "namespace", "status", "serviceType", "selector",
    "ports", "endpointInfo", "coreDnsExists", "probableCauses", "evidence", "suggestedActions"
})
public class ServiceDiagnosticResult {

    private Summary summary;
    private String resourceName;
    private String namespace;
    private String status;
    private String serviceType;
    private Map<String, String> selector;
    private List<ServicePort> ports;
    private List<String> probableCauses;
    private List<String> evidence;
    private List<String> suggestedActions;
    private EndpointInfo endpointInfo;
    private boolean coreDnsExists;

    public ServiceDiagnosticResult() {
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

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Map<String, String> getSelector() {
        return selector;
    }

    public void setSelector(Map<String, String> selector) {
        this.selector = selector;
    }

    public List<ServicePort> getPorts() {
        return ports;
    }

    public void setPorts(List<ServicePort> ports) {
        this.ports = ports;
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

    public EndpointInfo getEndpointInfo() {
        return endpointInfo;
    }

    public void setEndpointInfo(EndpointInfo endpointInfo) {
        this.endpointInfo = endpointInfo;
    }

    public boolean isCoreDnsExists() {
        return coreDnsExists;
    }

    public void setCoreDnsExists(boolean coreDnsExists) {
        this.coreDnsExists = coreDnsExists;
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
            this.resourceType = "Service";
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
     * Inner class representing a service port configuration.
     */
    @JsonPropertyOrder({"name", "protocol", "port", "targetPort", "nodePort"})
    public static class ServicePort {
        private String name;
        private String protocol;
        private int port;
        private Integer targetPort;
        private Integer nodePort;

        public ServicePort() {
        }

        // Getters and Setters

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public Integer getTargetPort() {
            return targetPort;
        }

        public void setTargetPort(Integer targetPort) {
            this.targetPort = targetPort;
        }

        public Integer getNodePort() {
            return nodePort;
        }

        public void setNodePort(Integer nodePort) {
            this.nodePort = nodePort;
        }
    }

    /**
     * Inner class representing endpoint information for the service.
     */
    @JsonPropertyOrder({"readyEndpoints", "notReadyEndpoints", "addresses"})
    public static class EndpointInfo {
        private int readyEndpoints;
        private int notReadyEndpoints;
        private List<String> addresses;

        public EndpointInfo() {
        }

        // Getters and Setters

        public int getReadyEndpoints() {
            return readyEndpoints;
        }

        public void setReadyEndpoints(int readyEndpoints) {
            this.readyEndpoints = readyEndpoints;
        }

        public int getNotReadyEndpoints() {
            return notReadyEndpoints;
        }

        public void setNotReadyEndpoints(int notReadyEndpoints) {
            this.notReadyEndpoints = notReadyEndpoints;
        }

        public List<String> getAddresses() {
            return addresses;
        }

        public void setAddresses(List<String> addresses) {
            this.addresses = addresses;
        }
    }
}
