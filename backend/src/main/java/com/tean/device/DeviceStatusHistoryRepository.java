package com.tean.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceStatusHistoryRepository extends JpaRepository<DeviceStatusHistory, Long> {
    List<DeviceStatusHistory> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
