package com.tean.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceStatusLogRepository extends JpaRepository<DeviceStatusLog, Long> {

    List<DeviceStatusLog> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
