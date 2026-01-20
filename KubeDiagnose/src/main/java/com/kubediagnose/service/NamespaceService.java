package com.kubediagnose.service;

import com.kubediagnose.model.NamespaceListResponse;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1NamespaceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Namespace discovery service. */
@Service
public class NamespaceService {

    private static final Logger logger = LoggerFactory.getLogger(NamespaceService.class);

    private final CoreV1Api coreV1Api;

    public NamespaceService(CoreV1Api coreV1Api) {
        this.coreV1Api = coreV1Api;
    }

    /** List namespaces sorted alphabetically. */
    public NamespaceListResponse listNamespaces() throws ApiException {
        logger.info("Fetching all namespaces from cluster");

        V1NamespaceList namespaceList = coreV1Api.listNamespace().execute();

        List<V1Namespace> namespaces = namespaceList.getItems() != null
                ? namespaceList.getItems()
                : new ArrayList<>();

        List<String> namespaceNames = namespaces.stream()
                .map(ns -> ns.getMetadata() != null ? ns.getMetadata().getName() : null)
                .filter(name -> name != null)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        logger.info("Found {} namespaces in cluster", namespaceNames.size());

        return new NamespaceListResponse(namespaceNames);
    }
}
