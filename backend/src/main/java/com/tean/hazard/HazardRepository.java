package com.tean.hazard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HazardRepository extends JpaRepository<HazardEntity, Long> {
    List<HazardEntity> findByDeviceId(Long deviceId);

    List<HazardEntity> findByOrgId(Long orgId);

    long countByDeviceIdAndStatus(Long deviceId, HazardEntity.Status status);
}
