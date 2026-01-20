package com.kubediagnose.rules;

import com.kubediagnose.model.PodDiagnosticResult;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1ContainerStatus;
import io.kubernetes.client.openapi.models.V1ContainerStateTerminated;
import io.kubernetes.client.openapi.models.V1ContainerStateWaiting;

import java.util.List;

/**
 * Collection of diagnostic rules for Kubernetes Pods.
 * Each rule checks for a specific failure condition and provides evidence and suggested actions.
 */
public class PodDiagnosticRules {

    // Threshold for considering restart count as "high"
    private static final int HIGH_RESTART_THRESHOLD = 5;

    /**
     * Checks if any container is in CrashLoopBackOff state.
     * CrashLoopBackOff indicates the container is repeatedly crashing after starting.
     *
     * @param pod The pod to check
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     */
    public static void checkCrashLoopBackOff(V1Pod pod, List<String> causes,
                                              List<String> evidence, List<String> actions) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return;
        }

        for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
            if (containerStatus.getState() != null && containerStatus.getState().getWaiting() != null) {
                V1ContainerStateWaiting waiting = containerStatus.getState().getWaiting();
                if ("CrashLoopBackOff".equals(waiting.getReason())) {
                    causes.add("Container '" + containerStatus.getName() + "' is in CrashLoopBackOff");
                    evidence.add("Container state: Waiting, Reason: CrashLoopBackOff");
                    evidence.add("Message: " + (waiting.getMessage() != null ? waiting.getMessage() : "No message"));
                    evidence.add("Restart count: " + containerStatus.getRestartCount());

                    actions.add("Check container logs: kubectl logs " + pod.getMetadata().getName() +
                               " -c " + containerStatus.getName() + " --previous");
                    actions.add("Review application startup logic and exit codes");
                    actions.add("Verify environment variables and configuration");
                    actions.add("Check if required dependencies or services are available");
                }
            }
        }
    }

    /**
     * Checks if any container has ImagePullBackOff or ErrImagePull error.
     * These errors indicate Kubernetes cannot pull the container image.
     *
     * @param pod The pod to check
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     */
    public static void checkImagePullErrors(V1Pod pod, List<String> causes,
                                            List<String> evidence, List<String> actions) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return;
        }

        for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
            if (containerStatus.getState() != null && containerStatus.getState().getWaiting() != null) {
                V1ContainerStateWaiting waiting = containerStatus.getState().getWaiting();
                String reason = waiting.getReason();

                if ("ImagePullBackOff".equals(reason) || "ErrImagePull".equals(reason)) {
                    causes.add("Container '" + containerStatus.getName() + "' cannot pull image: " + reason);
                    evidence.add("Container state: Waiting, Reason: " + reason);
                    evidence.add("Message: " + (waiting.getMessage() != null ? waiting.getMessage() : "No message"));
                    evidence.add("Image: " + containerStatus.getImage());

                    actions.add("Verify the image name and tag are correct");
                    actions.add("Check if the image exists in the registry");
                    actions.add("Ensure image pull secrets are configured if using private registry");
                    actions.add("Verify network connectivity to the container registry");
                }
            }
        }
    }

    /**
     * Checks if any container was terminated due to OOMKilled (Out of Memory).
     * OOMKilled indicates the container exceeded its memory limit.
     *
     * @param pod The pod to check
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     */
    public static void checkOOMKilled(V1Pod pod, List<String> causes,
                                      List<String> evidence, List<String> actions) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return;
        }

        for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
            // Check last terminated state
            if (containerStatus.getLastState() != null &&
                containerStatus.getLastState().getTerminated() != null) {
                V1ContainerStateTerminated terminated = containerStatus.getLastState().getTerminated();
                if ("OOMKilled".equals(terminated.getReason())) {
                    causes.add("Container '" + containerStatus.getName() + "' was OOMKilled (Out of Memory)");
                    evidence.add("Last termination reason: OOMKilled");
                    evidence.add("Exit code: " + terminated.getExitCode());
                    evidence.add("Finished at: " + (terminated.getFinishedAt() != null ?
                                 terminated.getFinishedAt().toString() : "Unknown"));

                    actions.add("Increase memory limits in pod spec");
                    actions.add("Profile application memory usage to find leaks");
                    actions.add("Optimize application memory consumption");
                    actions.add("Consider using vertical pod autoscaler");
                }
            }

            // Also check current terminated state
            if (containerStatus.getState() != null &&
                containerStatus.getState().getTerminated() != null) {
                V1ContainerStateTerminated terminated = containerStatus.getState().getTerminated();
                if ("OOMKilled".equals(terminated.getReason())) {
                    causes.add("Container '" + containerStatus.getName() + "' is currently OOMKilled");
                    evidence.add("Current termination reason: OOMKilled");
                    evidence.add("Exit code: " + terminated.getExitCode());

                    actions.add("Increase memory limits in pod spec");
                    actions.add("Profile application memory usage");
                }
            }
        }
    }

    /**
     * Checks if any container has failed liveness or readiness probes.
     * Probe failures can cause containers to be restarted or traffic to be withheld.
     *
     * @param pod The pod to check
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     */
    public static void checkProbeFailures(V1Pod pod, List<String> causes,
                                          List<String> evidence, List<String> actions) {
        if (pod.getStatus() == null) {
            return;
        }

        // Check conditions for probe-related failures
        if (pod.getStatus().getConditions() != null) {
            pod.getStatus().getConditions().forEach(condition -> {
                if ("False".equals(condition.getStatus())) {
                    if ("Ready".equals(condition.getType()) && condition.getReason() != null) {
                        if (condition.getReason().contains("Probe") ||
                            condition.getMessage() != null && condition.getMessage().contains("probe")) {
                            causes.add("Readiness probe is failing");
                            evidence.add("Condition: Ready=False, Reason: " + condition.getReason());
                            evidence.add("Message: " + condition.getMessage());

                            actions.add("Check the readiness probe configuration");
                            actions.add("Verify the probe endpoint/command is working");
                            actions.add("Increase probe timeout or failure threshold if needed");
                        }
                    }
                }
            });
        }

        // Check container statuses for probe-related issues in messages
        if (pod.getStatus().getContainerStatuses() != null) {
            for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
                // High restart count might indicate probe failures
                if (containerStatus.getRestartCount() > 0 && !containerStatus.getReady()) {
                    if (containerStatus.getLastState() != null &&
                        containerStatus.getLastState().getTerminated() != null) {
                        V1ContainerStateTerminated terminated = containerStatus.getLastState().getTerminated();
                        // Exit code 137 often indicates killed by SIGKILL (liveness probe failure)
                        if (terminated.getExitCode() != null && terminated.getExitCode() == 137) {
                            causes.add("Container '" + containerStatus.getName() +
                                       "' may be killed by liveness probe (exit code 137)");
                            evidence.add("Last termination exit code: 137 (SIGKILL)");
                            evidence.add("Container restart count: " + containerStatus.getRestartCount());

                            actions.add("Review liveness probe configuration");
                            actions.add("Increase initialDelaySeconds if application needs more startup time");
                            actions.add("Check application health endpoint response time");
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if any container has a high restart count.
     * High restart count indicates recurring issues with the container.
     *
     * @param pod The pod to check
     * @param causes List to add probable causes to
     * @param evidence List to add evidence to
     * @param actions List to add suggested actions to
     * @return Total restart count across all containers
     */
    public static int checkHighRestartCount(V1Pod pod, List<String> causes,
                                            List<String> evidence, List<String> actions) {
        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return 0;
        }

        int totalRestarts = 0;
        for (V1ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
            int restarts = containerStatus.getRestartCount();
            totalRestarts += restarts;

            if (restarts >= HIGH_RESTART_THRESHOLD) {
                causes.add("Container '" + containerStatus.getName() +
                           "' has high restart count: " + restarts);
                evidence.add("Container '" + containerStatus.getName() +
                            "' restart count: " + restarts);
                evidence.add("Ready status: " + containerStatus.getReady());

                actions.add("Check previous container logs: kubectl logs " +
                           pod.getMetadata().getName() + " -c " + containerStatus.getName() + " --previous");
                actions.add("Review application stability and error handling");
                actions.add("Check resource limits (CPU/Memory)");
            }
        }

        return totalRestarts;
    }

    /**
     * Builds container status DTOs from pod container statuses.
     *
     * @param pod The pod to extract container statuses from
     * @return List of ContainerStatus DTOs
     */
    public static java.util.List<PodDiagnosticResult.ContainerStatus> buildContainerStatuses(V1Pod pod) {
        java.util.List<PodDiagnosticResult.ContainerStatus> statuses = new java.util.ArrayList<>();

        if (pod.getStatus() == null || pod.getStatus().getContainerStatuses() == null) {
            return statuses;
        }

        for (V1ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
            PodDiagnosticResult.ContainerStatus status = new PodDiagnosticResult.ContainerStatus();
            status.setName(cs.getName());
            status.setRestartCount(cs.getRestartCount());
            status.setReady(cs.getReady() != null && cs.getReady());

            // Determine state and reason
            if (cs.getState() != null) {
                if (cs.getState().getRunning() != null) {
                    status.setState("Running");
                } else if (cs.getState().getWaiting() != null) {
                    status.setState("Waiting");
                    status.setReason(cs.getState().getWaiting().getReason());
                    status.setMessage(cs.getState().getWaiting().getMessage());
                } else if (cs.getState().getTerminated() != null) {
                    status.setState("Terminated");
                    status.setReason(cs.getState().getTerminated().getReason());
                    status.setMessage(cs.getState().getTerminated().getMessage());
                }
            }

            statuses.add(status);
        }

        return statuses;
    }
}
