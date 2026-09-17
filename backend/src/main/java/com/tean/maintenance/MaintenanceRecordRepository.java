package com.tean.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    Optional<MaintenanceRecord> findByRecordNo(String recordNo);

    List<MaintenanceRecord> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
