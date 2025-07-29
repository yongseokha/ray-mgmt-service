package com.example.raymgmt.RayCluster;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RayClusterBaseVO {

    private String apiVersion = "ray.io/v1";
    private String kind = "RayCluster";
    private Map<String, Object> metadata;
    private Map<String, Object> spec;

    @Data
    public static class Metadata {
        private String name;
        private String namespace;
        private Map<String, String> labels;
    }

    @Data
    public static class Spec {
        private String rayVersion;
        private HeadGroupSpec headGroupSpec;
        private List<WorkerGroupSpec> workerGroupSpecs;
    }

    @Data
    public static class HeadGroupSpec {
        private String serviceType;
        private Integer replicas;
        private List<Container> containers;
        private Map<String, String> rayStartParams;
    }

    @Data
    public static class WorkerGroupSpec {
        private String groupName;
        private Integer replicas;
        private List<Container> containers;
        private Map<String, String> rayStartParams;
    }

    @Data
    public static class Container {
        private String name;
        private String image;
        private List<String> command;
        private List<String> args;
        private List<EnvVar> env;
        private Resource resources;
        private List<VolumeMount> volumeMounts;
        private List<ContainerPort> ports;
    }

    @Data
    public static class EnvVar {
        private String name;
        private String value;
    }

    @Data
    public static class VolumeMount {
        private String name;
        private String mountPath;
    }

    @Data
    public static class ContainerPort {
        private Integer containerPort;
        private String protocol; // "TCP" or "UDP"
    }

    @Data
    public static class Resource {
        private ResourceValue limits;
        private ResourceValue requests;
    }

    @Data
    public static class ResourceValue {
        private String cpu;    // ex) "1", "500m"
        private String memory; // ex) "512Mi", "2Gi"
    }
}
