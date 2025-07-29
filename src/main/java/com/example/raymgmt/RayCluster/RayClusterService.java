package com.example.raymgmt.RayCluster;

import java.util.List;

public interface RayClusterService {

    void createCluster(RayClusterBaseVO vo);

    List<RayClusterBaseVO> getClusterList();

    RayClusterBaseVO getCluster(String name);

    void updateCluster(String name, RayClusterBaseVO vo);

    void deleteCluster(String name);

    RayClusterRequest updateCluster(String name, RayClusterRequest newVO);
    boolean isResourceIncreased(RayClusterRequest oldVO, RayClusterRequest newVO);
    boolean deployToKubernetes(RayClusterRequest vo);

}
