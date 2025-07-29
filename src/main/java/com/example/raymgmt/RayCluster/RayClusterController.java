package com.example.raymgmt.RayCluster;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
public class RayClusterController {

    private final RayClusterService rayClusterService;

    // ✅ 클러스터 생성
    @PostMapping
    public ResponseEntity<String> createCluster(@RequestBody RayClusterRequest request) {
        rayClusterService.createCluster(request);
        return ResponseEntity.ok("✅ 클러스터 생성 요청 완료");
    }

    // ✅ 전체 클러스터 목록 조회
    @GetMapping
    public ResponseEntity<List<RayClusterBaseVO>> getClusters() {
        return ResponseEntity.ok(rayClusterService.getClusterList());
    }

    // ✅ 단일 클러스터 조회
    @GetMapping("/{name}")
    public ResponseEntity<RayClusterBaseVO> getCluster(@PathVariable String name) {
        RayClusterBaseVO cluster = rayClusterService.getCluster(name);
        if (cluster == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cluster);
    }

    // ✅ 클러스터 수정 (status 판단 + 자동 승인 또는 승인 대기)
    @PatchMapping("/{name}")
    public ResponseEntity<String> updateCluster(@PathVariable String name,
                                                @RequestBody RayClusterRequest updatedRequest) {
        RayClusterRequest result = rayClusterService.updateCluster(name, updatedRequest);
        return ResponseEntity.ok("✅ 클러스터 수정 완료 (status: " + result.getStatus() + ")");
    }

    // ✅ 클러스터 삭제
    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteCluster(@PathVariable String name) {
        rayClusterService.deleteCluster(name);
        return ResponseEntity.ok("🗑️ 클러스터 삭제 완료");
    }

    // ✅ 관리자 승인 후 K8s API 호출 (Webhook 트리거)
    @PatchMapping("/{name}/approve")
    public ResponseEntity<String> approveCluster(@PathVariable String name) {
        RayClusterRequest request = (RayClusterRequest) rayClusterService.getCluster(name);

        if (request == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"approval".equalsIgnoreCase(request.getStatus())) {
            return ResponseEntity.badRequest().body("❌ status 값이 'approval'이 아닙니다.");
        }

        boolean result = rayClusterService.deployToKubernetes(request);

        if (result) {
            return ResponseEntity.ok("🚀 Kubernetes에 RayCluster 리소스 배포 완료");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Kubernetes API 호출 실패");
        }
    }
}
