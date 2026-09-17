package com.tean.inspection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionRecordRepository extends JpaRepository<InspectionRecord, Long> {
    List<InspectionRecord> findByDeviceIdOrderByInspectedAtDesc(Long deviceId);
}
