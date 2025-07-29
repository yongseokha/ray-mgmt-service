package com.example.raymgmt.RayCluster;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
public class RayClusterController {

    private final RayClusterService rayClusterService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCluster(@RequestBody RayClusterRequest request) {
        rayClusterService.createCluster(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "✅ 클러스터 생성 요청 완료");
        response.put("status", request.getStatus());
        response.put("webhookUrl", "http://your-k8s-api-endpoint/api/v1/raycluster");

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RayClusterBaseVO>> getClusters() {
        return ResponseEntity.ok(rayClusterService.getClusterList());
    }

    @GetMapping("/{name}")
    public ResponseEntity<Map<String, Object>> getClusterDetail(@PathVariable String name) {
        RayClusterRequest vo = (RayClusterRequest) rayClusterService.getCluster(name);
        if (vo == null) return ResponseEntity.notFound().build();

        Map<String, Object> response = new HashMap<>();
        response.put("cluster", vo);
        response.put("webhookUrl", "http://your-k8s-api-endpoint/api/v1/raycluster");

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{name}")
    public ResponseEntity<String> updateCluster(@PathVariable String name,
                                                @RequestBody RayClusterRequest updatedRequest) {
        RayClusterRequest result = rayClusterService.updateCluster(name, updatedRequest);
        return ResponseEntity.ok("✅ 클러스터 수정 완료 (status: " + result.getStatus() + ")");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteCluster(@PathVariable String name) {
        rayClusterService.deleteCluster(name);
        return ResponseEntity.ok("🗑️ 클러스터 삭제 완료");
    }

    @PatchMapping("/{name}/approve")
    public ResponseEntity<String> approveCluster(@PathVariable String name) {
        RayClusterRequest request = (RayClusterRequest) rayClusterService.getCluster(name);
        if (request == null) return ResponseEntity.notFound().build();

        if (!"approval".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.badRequest().body("❌ status 값이 'approval'이 아닙니다.");
        }

        boolean result = rayClusterService.deployToKubernetes(request);

        if (result) return ResponseEntity.ok("🚀 Kubernetes에 RayCluster 리소스 배포 완료");
        else return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("❌ Kubernetes API 호출 실패");
    }
}
