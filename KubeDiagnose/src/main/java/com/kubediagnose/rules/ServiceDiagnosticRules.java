package com.kubediagnose.rules;

import com.kubediagnose.model.ServiceDiagnosticResult;
import io.kubernetes.client.openapi.models.*;

import java.util.*;

/**
 * Collection of diagnostic rules for Kubernetes Services.
 * Each rule checks for a specific networking or configuration issue.
 */
public class ServiceDiagnosticRules {

    /**
     * Checks if the service selector matches any pods in the namespace.
     * A selector mismatch means the service won't route traffic to any pods.
     *
     * @param service The service to check
     * @param pods List of pods in the same namespace
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     * @return true if there is a selector mismatch
     */
    public static boolean checkSelectorMismatch(V1Service service, List<V1Pod> pods,
                                                List<String> causes, List<String> evidence,
                                                List<String> actions) {
        if (service.getSpec() == null || service.getSpec().getSelector() == null) {
            causes.add("Service has no selector defined");
            evidence.add("Service spec has no selector");
            actions.add("Add a selector to the service that matches target pod labels");
            return true;
        }

        Map<String, String> selector = service.getSpec().getSelector();
        if (selector.isEmpty()) {
            causes.add("Service has empty selector");
            evidence.add("Service selector is empty: {}");
            actions.add("Define pod labels in selector that match your target pods");
            return true;
        }

        // Count matching pods
        int matchingPods = 0;
        List<String> podLabelInfo = new ArrayList<>();

        for (V1Pod pod : pods) {
            if (pod.getMetadata() == null || pod.getMetadata().getLabels() == null) {
                continue;
            }

            Map<String, String> podLabels = pod.getMetadata().getLabels();
            boolean matches = selector.entrySet().stream()
                    .allMatch(entry -> entry.getValue().equals(podLabels.get(entry.getKey())));

            if (matches) {
                matchingPods++;
            } else {
                // Collect info about non-matching pods for evidence
                if (podLabelInfo.size() < 3) { // Limit evidence size
                    podLabelInfo.add("Pod '" + pod.getMetadata().getName() +
                                    "' labels: " + podLabels);
                }
            }
        }

        if (matchingPods == 0) {
            causes.add("Service selector does not match any pods");
            evidence.add("Service selector: " + selector);
            evidence.add("Total pods in namespace: " + pods.size());
            evidence.add("Matching pods: 0");
            podLabelInfo.forEach(evidence::add);

            actions.add("Verify the service selector labels match pod labels");
            actions.add("Use 'kubectl get pods --show-labels' to see pod labels");
            actions.add("Update service selector or pod labels to match");
            return true;
        }

        return false;
    }

    /**
     * Checks if the service has any endpoints.
     * No endpoints means no pods are backing the service.
     *
     * @param endpoints The endpoints object for the service
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     * @return EndpointInfo with endpoint details
     */
    public static ServiceDiagnosticResult.EndpointInfo checkNoEndpoints(V1Endpoints endpoints,
                                                                        List<String> causes,
                                                                        List<String> evidence,
                                                                        List<String> actions) {
        ServiceDiagnosticResult.EndpointInfo endpointInfo = new ServiceDiagnosticResult.EndpointInfo();
        List<String> addresses = new ArrayList<>();
        int readyCount = 0;
        int notReadyCount = 0;

        if (endpoints == null || endpoints.getSubsets() == null || endpoints.getSubsets().isEmpty()) {
            causes.add("Service has no endpoints");
            evidence.add("No endpoint subsets found for this service");
            actions.add("Ensure pods matching the service selector are running");
            actions.add("Check if pods are in Ready state");
            actions.add("Verify service selector matches pod labels");

            endpointInfo.setReadyEndpoints(0);
            endpointInfo.setNotReadyEndpoints(0);
            endpointInfo.setAddresses(addresses);
            return endpointInfo;
        }

        for (V1EndpointSubset subset : endpoints.getSubsets()) {
            // Count ready addresses
            if (subset.getAddresses() != null) {
                for (V1EndpointAddress addr : subset.getAddresses()) {
                    readyCount++;
                    addresses.add(addr.getIp() + " (Ready)");
                }
            }

            // Count not-ready addresses
            if (subset.getNotReadyAddresses() != null) {
                for (V1EndpointAddress addr : subset.getNotReadyAddresses()) {
                    notReadyCount++;
                    addresses.add(addr.getIp() + " (NotReady)");
                }
            }
        }

        if (readyCount == 0 && notReadyCount > 0) {
            causes.add("Service has endpoints but none are ready");
            evidence.add("Ready endpoints: 0");
            evidence.add("Not ready endpoints: " + notReadyCount);
            actions.add("Check pod readiness probes");
            actions.add("Ensure pods are healthy and passing readiness checks");
        }

        endpointInfo.setReadyEndpoints(readyCount);
        endpointInfo.setNotReadyEndpoints(notReadyCount);
        endpointInfo.setAddresses(addresses);
        return endpointInfo;
    }

    /**
     * Checks for port mismatches between service and pod container ports.
     * A port mismatch means traffic might not reach the application correctly.
     *
     * @param service The service to check
     * @param pods List of matching pods
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     */
    public static void checkPortMismatch(V1Service service, List<V1Pod> pods,
                                         List<String> causes, List<String> evidence,
                                         List<String> actions) {
        if (service.getSpec() == null || service.getSpec().getPorts() == null) {
            return;
        }

        // Collect all container ports from matching pods
        Set<Integer> containerPorts = new HashSet<>();
        for (V1Pod pod : pods) {
            if (pod.getSpec() == null || pod.getSpec().getContainers() == null) {
                continue;
            }
            for (V1Container container : pod.getSpec().getContainers()) {
                if (container.getPorts() != null) {
                    for (V1ContainerPort port : container.getPorts()) {
                        containerPorts.add(port.getContainerPort());
                    }
                }
            }
        }

        if (containerPorts.isEmpty() && !pods.isEmpty()) {
            evidence.add("Warning: No container ports explicitly defined in pods");
            actions.add("Consider explicitly defining containerPort in pod spec for clarity");
        }

        // Check each service port's targetPort
        for (V1ServicePort servicePort : service.getSpec().getPorts()) {
            Integer targetPort = null;

            // Get target port (can be Integer or String)
            if (servicePort.getTargetPort() != null) {
                if (servicePort.getTargetPort().isInteger()) {
                    targetPort = servicePort.getTargetPort().getIntValue();
                } else {
                    // Named port - we'd need to resolve it, skip for now
                    evidence.add("Service uses named port '" + servicePort.getTargetPort().getStrValue() +
                                "' - ensure pod has matching port name");
                    continue;
                }
            } else {
                // If targetPort not specified, it defaults to port
                targetPort = servicePort.getPort();
            }

            if (!containerPorts.isEmpty() && !containerPorts.contains(targetPort)) {
                causes.add("Service targetPort " + targetPort + " may not match any container port");
                evidence.add("Service port " + servicePort.getPort() + " -> targetPort " + targetPort);
                evidence.add("Container ports found: " + containerPorts);

                actions.add("Verify service targetPort matches container port");
                actions.add("Update service targetPort to match actual container port");
            }
        }
    }

    /**
     * Checks if CoreDNS is running in the cluster.
     * CoreDNS is essential for service discovery in Kubernetes.
     *
     * @param coreDnsPods List of CoreDNS pods (typically in kube-system namespace)
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     * @return true if CoreDNS exists and is running
     */
    public static boolean checkCoreDnsExists(List<V1Pod> coreDnsPods, List<String> causes,
                                             List<String> evidence, List<String> actions) {
        if (coreDnsPods == null || coreDnsPods.isEmpty()) {
            causes.add("CoreDNS pods not found in kube-system namespace");
            evidence.add("No pods with label 'k8s-app=kube-dns' found in kube-system");
            actions.add("Check CoreDNS deployment: kubectl get deployment coredns -n kube-system");
            actions.add("Verify DNS is configured correctly in the cluster");
            return false;
        }

        int runningCount = 0;
        for (V1Pod pod : coreDnsPods) {
            if (pod.getStatus() != null && "Running".equals(pod.getStatus().getPhase())) {
                runningCount++;
            }
        }

        if (runningCount == 0) {
            causes.add("CoreDNS pods exist but none are running");
            evidence.add("CoreDNS pods found: " + coreDnsPods.size());
            evidence.add("Running CoreDNS pods: 0");
            actions.add("Check CoreDNS pod status: kubectl get pods -n kube-system -l k8s-app=kube-dns");
            actions.add("Check CoreDNS logs: kubectl logs -n kube-system -l k8s-app=kube-dns");
            return false;
        }

        evidence.add("CoreDNS is running (" + runningCount + " pod(s))");
        return true;
    }

    /**
     * Builds service port DTOs from service spec.
     *
     * @param service The service to extract ports from
     * @return List of ServicePort DTOs
     */
    public static List<ServiceDiagnosticResult.ServicePort> buildServicePorts(V1Service service) {
        List<ServiceDiagnosticResult.ServicePort> ports = new ArrayList<>();

        if (service.getSpec() == null || service.getSpec().getPorts() == null) {
            return ports;
        }

        for (V1ServicePort sp : service.getSpec().getPorts()) {
            ServiceDiagnosticResult.ServicePort port = new ServiceDiagnosticResult.ServicePort();
            port.setName(sp.getName());
            port.setProtocol(sp.getProtocol());
            port.setPort(sp.getPort());

            if (sp.getTargetPort() != null) {
                if (sp.getTargetPort().isInteger()) {
                    port.setTargetPort(sp.getTargetPort().getIntValue());
                }
            }

            if (sp.getNodePort() != null) {
                port.setNodePort(sp.getNodePort());
            }

            ports.add(port);
        }

        return ports;
    }
}
