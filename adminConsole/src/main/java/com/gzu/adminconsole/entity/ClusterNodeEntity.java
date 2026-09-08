package com.gzu.adminconsole.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 集群微服务容器节点。
 */
@Entity
@Table(name = "cluster_node")
public class ClusterNodeEntity {

    @Id
    @Column(name = "node_id", length = 64)
    private String id;

    @Column(name = "zone", length = 64)
    private String zone;

    @Column(name = "role", length = 128)
    private String role;

    @Column(name = "containers")
    private int containers;

    @Column(name = "cpu")
    private double cpu;

    @Column(name = "gpu")
    private double gpu;

    @Column(name = "latency_ms")
    private int latencyMs;

    @Column(name = "status", length = 16)
    private String status;

    protected ClusterNodeEntity() {
    }

    public ClusterNodeEntity(String id, String zone, String role, int containers, double cpu, double gpu,
                             int latencyMs, String status) {
        this.id = id;
        this.zone = zone;
        this.role = role;
        this.containers = containers;
        this.cpu = cpu;
        this.gpu = gpu;
        this.latencyMs = latencyMs;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public int getContainers() { return containers; }
    public void setContainers(int containers) { this.containers = containers; }
    public double getCpu() { return cpu; }
    public void setCpu(double cpu) { this.cpu = cpu; }
    public double getGpu() { return gpu; }
    public void setGpu(double gpu) { this.gpu = gpu; }
    public int getLatencyMs() { return latencyMs; }
    public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
