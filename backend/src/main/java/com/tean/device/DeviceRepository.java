package com.tean.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long>, JpaSpecificationExecutor<DeviceEntity> {
}
