package com.tean.hazard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface HazardRepository extends JpaRepository<Hazard, Long> {

    List<Hazard> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    List<Hazard> findByStatusAndDeviceIdIn(HazardStatus status, Collection<Long> deviceIds);

    List<Hazard> findByStatus(HazardStatus status);
}
