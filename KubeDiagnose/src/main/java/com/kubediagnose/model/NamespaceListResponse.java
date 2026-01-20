package com.kubediagnose.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * DTO representing the list of available namespaces in the cluster.
 */
@JsonPropertyOrder({"total", "namespaces"})
public class NamespaceListResponse {

    private int total;
    private List<String> namespaces;

    public NamespaceListResponse() {
    }

    public NamespaceListResponse(List<String> namespaces) {
        this.namespaces = namespaces;
        this.total = namespaces != null ? namespaces.size() : 0;
    }

    // Getters and Setters

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(List<String> namespaces) {
        this.namespaces = namespaces;
        this.total = namespaces != null ? namespaces.size() : 0;
    }
}
