package com.example.raymgmt.RayCluster;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RayClusterServiceImpl implements RayClusterService {

    private final Map<String, RayClusterRequest> clusterStore = new HashMap<>(); // 임시 저장소
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String K8S_API_URL = "http://your-k8s-api-endpoint/api/v1/raycluster";

    // ✅ 클러스터 생성
    @Override
    public void createCluster(RayClusterBaseVO vo) {
        RayClusterRequest request = (RayClusterRequest) vo;
        clusterStore.put(request.getMetadata().get("name").toString(), request);
    }

    // ✅ 클러스터 목록 조회
    @Override
    public List<RayClusterBaseVO> getClusterList() {
        return new ArrayList<>(clusterStore.values());
    }

    // ✅ 단일 클러스터 조회
    @Override
    public RayClusterBaseVO getCluster(String name) {
        return clusterStore.get(name);
    }

    // ✅ 기존 방식의 업데이트 (컨트롤러에서 BaseVO로 호출 시)
    @Override
    public void updateCluster(String name, RayClusterBaseVO newVO) {
        updateCluster(name, (RayClusterRequest) newVO);
    }

    // ✅ 새 방식의 업데이트: 변경된 요청 받아 상태 결정 및 배포 처리
    @Override
    public RayClusterRequest updateCluster(String name, RayClusterRequest newVO) {
        RayClusterRequest existing = clusterStore.get(name);
        if (existing == null) {
            throw new RuntimeException("Cluster not found: " + name);
        }

        boolean needsApproval = isResourceIncreased(existing, newVO);

        if (needsApproval) {
            newVO.setStatus("request");
        } else {
            newVO.setStatus("approval");
            deployToKubernetes(newVO); // 자동 배포
        }

        clusterStore.put(name, newVO);
        return newVO;
    }

    // ✅ 클러스터 삭제
    @Override
    public void deleteCluster(String name) {
        clusterStore.remove(name);
    }

    // ✅ 리소스 증가 여부 판단 (limits.cpu 또는 memory 증가 시 true)
    @Override
    public boolean isResourceIncreased(RayClusterRequest oldVO, RayClusterRequest newVO) {
        Map<String, Object> oldSpec = oldVO.getSpec();
        Map<String, Object> newSpec = newVO.getSpec();

        try {
            Map<String, Object> oldHead = (Map<String, Object>) oldSpec.get("headGroupSpec");
            Map<String, Object> newHead = (Map<String, Object>) newSpec.get("headGroupSpec");

            List<Map<String, Object>> oldContainers = (List<Map<String, Object>>) oldHead.get("containers");
            List<Map<String, Object>> newContainers = (List<Map<String, Object>>) newHead.get("containers");

            Map<String, Object> oldResources = (Map<String, Object>) oldContainers.get(0).get("resources");
            Map<String, Object> newResources = (Map<String, Object>) newContainers.get(0).get("resources");

            Map<String, String> oldLimits = (Map<String, String>) oldResources.get("limits");
            Map<String, String> newLimits = (Map<String, String>) newResources.get("limits");

            return isIncreased(oldLimits.get("cpu"), newLimits.get("cpu")) ||
                    isIncreased(oldLimits.get("memory"), newLimits.get("memory"));

        } catch (Exception e) {
            throw new RuntimeException("자원 비교 실패: " + e.getMessage());
        }
    }

    private boolean isIncreased(String oldVal, String newVal) {
        if (oldVal == null || newVal == null) return false;
        return !oldVal.equals(newVal); // 향후 단위 변환 비교 가능
    }

    // ✅ Webhook 호출 (status=approval인 경우 K8s API 호출)
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
