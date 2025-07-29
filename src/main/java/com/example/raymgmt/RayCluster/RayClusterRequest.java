package com.example.raymgmt.RayCluster;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "RayCluster 리소스 요청 객체 (관리용 메타데이터 포함)")
public class RayClusterRequest extends RayClusterBaseVO {

    @Schema(description = "플랫폼 프로젝트 ID", example = "proj-001")
    private String prjId;

    @Schema(description = "플랫폼 서비스 ID", example = "svc-ray")
    private String svcId;

    @Schema(description = "리소스 ID", example = "raycluster-123")
    private String rscId;

    @Schema(description = "등록자 계정 ID", example = "admin-user")
    private String createdBy;

    @Schema(description = "등록 일시 (ISO 8601)", example = "2025-07-29T22:00:00Z")
    private String createdAt;

    @Schema(description = "현재 상태 (request: 요청 중 / approval: 승인됨)", example = "request")
    private String status;
}
