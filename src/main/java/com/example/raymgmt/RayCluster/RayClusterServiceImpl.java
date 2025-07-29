package com.example.raymgmt.RayCluster;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@Service
public class RayClusterServiceImpl implements RayClusterService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String K8S_API_URL = "http://your-k8s-api-endpoint/api/v1/raycluster";

    private RayClusterRequest getClusterByRscId(String rscId) {
        throw new UnsupportedOperationException("getClusterByRscId는 DB 연동 후 구현 필요");
    }

    @Override
    public void createCluster(RayClusterBaseVO vo) {
        if (!(vo instanceof RayClusterRequest)) throw new IllegalArgumentException("RayClusterRequest 타입 필요");

        RayClusterRequest request = (RayClusterRequest) vo;
        if ("approval".equalsIgnoreCase(request.getStatus())) {
            deployToKubernetes(request);
        }
    }

    @Override
    public List<RayClusterBaseVO> getClusterList() {
        throw new UnsupportedOperationException("getClusterList는 DB 연동 후 구현 필요");
    }

    @Override
    public RayClusterBaseVO getCluster(String name) {
        throw new UnsupportedOperationException("getCluster는 DB 연동 후 구현 필요");
    }

    @Override
    public void updateCluster(String name, RayClusterBaseVO vo) {
        updateCluster(name, (RayClusterRequest) vo);
    }

    @Override
    public RayClusterRequest updateCluster(String name, RayClusterRequest newVO) {
        RayClusterRequest existing = getClusterByRscId(newVO.getRscId());
        boolean needsApproval = isResourceIncreased(existing, newVO);

        if (needsApproval) {
            newVO.setStatus("request");
        } else {
            newVO.setStatus("approval");
            deployToKubernetes(newVO);
        }

        return newVO;
    }

    @Override
    public void deleteCluster(String name) {
        throw new UnsupportedOperationException("deleteCluster는 DB 연동 후 구현 필요");
    }

    @Override
    public boolean isResourceIncreased(RayClusterRequest oldVO, RayClusterRequest newVO) {
        try {
            Map<String, Object> oldSpec = oldVO.getSpec();
            Map<String, Object> newSpec = newVO.getSpec();

            // ✅ HeadGroup 비교
            boolean headIncreased = compareGroupSpec(
                    (Map<String, Object>) oldSpec.get("headGroupSpec"),
                    (Map<String, Object>) newSpec.get("headGroupSpec")
            );

            // ✅ WorkerGroups 비교
            List<Map<String, Object>> oldWorkers = (List<Map<String, Object>>) oldSpec.get("workerGroupSpecs");
            List<Map<String, Object>> newWorkers = (List<Map<String, Object>>) newSpec.get("workerGroupSpecs");

            boolean workerIncreased = false;
            if (oldWorkers != null && newWorkers != null) {
                for (int i = 0; i < Math.min(oldWorkers.size(), newWorkers.size()); i++) {
                    if (compareGroupSpec(oldWorkers.get(i), newWorkers.get(i))) {
                        workerIncreased = true;
                        break;
                    }
                }
            }

            return headIncreased || workerIncreased;

        } catch (Exception e) {
            throw new RuntimeException("자원 비교 실패: " + e.getMessage());
        }
    }

    private boolean compareGroupSpec(Map<String, Object> oldGroup, Map<String, Object> newGroup) {
        List<Map<String, Object>> oldContainers = (List<Map<String, Object>>) oldGroup.get("containers");
        List<Map<String, Object>> newContainers = (List<Map<String, Object>>) newGroup.get("containers");

        if (oldContainers == null || newContainers == null) return false;

        for (int i = 0; i < Math.min(oldContainers.size(), newContainers.size()); i++) {
            Map<String, Object> oldResources = (Map<String, Object>) oldContainers.get(i).get("resources");
            Map<String, Object> newResources = (Map<String, Object>) newContainers.get(i).get("resources");

            if (oldResources == null || newResources == null) continue;

            Map<String, String> oldLimits = (Map<String, String>) oldResources.get("limits");
            Map<String, String> newLimits = (Map<String, String>) newResources.get("limits");

            Map<String, String> newRequests = (Map<String, String>) newResources.get("requests");

            if (isIncreased(oldLimits.get("cpu"), newLimits.get("cpu")) ||
                    isIncreased(oldLimits.get("memory"), newLimits.get("memory"))) {
                return true;
            }

            if (!isRequestsWithinLimits(newRequests, newLimits)) {
                throw new RuntimeException("❌ 요청한 requests 값이 limits를 초과합니다.");
            }
        }

        return false;
    }

    private boolean isRequestsWithinLimits(Map<String, String> requests, Map<String, String> limits) {
        if (requests == null || limits == null) return true;

        BigDecimal reqCpu = parseResourceValue(requests.get("cpu"));
        BigDecimal limCpu = parseResourceValue(limits.get("cpu"));

        BigDecimal reqMem = parseResourceValue(requests.get("memory"));
        BigDecimal limMem = parseResourceValue(limits.get("memory"));

        return reqCpu.compareTo(limCpu) <= 0 && reqMem.compareTo(limMem) <= 0;
    }

    private boolean isIncreased(String oldVal, String newVal) {
        if (oldVal == null || newVal == null) return false;

        try {
            BigDecimal oldNum = parseResourceValue(oldVal);
            BigDecimal newNum = parseResourceValue(newVal);
            return newNum.compareTo(oldNum) > 0;
        } catch (Exception e) {
            return !oldVal.equals(newVal);
        }
    }

    private BigDecimal parseResourceValue(String value) {
        if (value == null) return BigDecimal.ZERO;
        if (value.endsWith("m")) {
            return new BigDecimal(value.replace("m", "")).divide(BigDecimal.valueOf(1000));
        } else if (value.endsWith("Gi")) {
            return new BigDecimal(value.replace("Gi", "")).multiply(BigDecimal.valueOf(1024));
        } else if (value.endsWith("Mi")) {
            return new BigDecimal(value.replace("Mi", ""));
        } else {
            return new BigDecimal(value);
        }
    }

    @Override
    public boolean deployToKubernetes(RayClusterRequest vo) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RayClusterRequest> entity = new HttpEntity<>(vo, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    K8S_API_URL, entity, String.class
            );

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            throw new RuntimeException("Webhook 호출 실패: " + e.getMessage());
        }
    }
}