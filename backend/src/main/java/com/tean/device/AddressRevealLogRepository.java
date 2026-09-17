package com.tean.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRevealLogRepository extends JpaRepository<AddressRevealLog, Long> {
    List<AddressRevealLog> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);
}
