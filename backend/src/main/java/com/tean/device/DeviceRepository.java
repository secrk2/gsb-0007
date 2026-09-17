package com.tean.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {

    List<Device> findByOrgIdIn(Collection<Long> orgIds);

    long countByOrgId(Long orgId);

    long countByOrgIdAndStatus(Long orgId, DeviceStatus status);

    long countByStatus(DeviceStatus status);

    /** 临期：在用到期日在 [from, to] 之间 */
    List<Device> findByStatusAndNextInspectionDateBetween(DeviceStatus status, LocalDate from, LocalDate to);

    /** 逾期：到期日早于 today 且未报废 */
    List<Device> findByStatusInAndNextInspectionDateBefore(List<DeviceStatus> statuses, LocalDate today);
}
