package com.tean.hazard;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 隐患模块 DTO。
 */
public final class HazardDtos {

    private HazardDtos() {
    }

    public record HazardItem(
            Long id,
            Long deviceId,
            String deviceName,
            String orgName,
            String title,
            HazardLevel level,
            String levelLabel,
            HazardStatus status,
            String statusLabel,
            LocalDate deadline,
            boolean overdue,
            LocalDateTime createdAt) {

        public static HazardItem of(Hazard h, String deviceName, String orgName, LocalDate today) {
            boolean overdue = h.getStatus() == HazardStatus.OPEN
                    && h.getDeadline() != null && h.getDeadline().isBefore(today);
            return new HazardItem(
                    h.getId(), h.getDeviceId(), deviceName, orgName,
                    h.getTitle(), h.getLevel(), h.getLevel().getLabel(),
                    h.getStatus(), h.getStatus().getLabel(),
                    h.getDeadline(), overdue, h.getCreatedAt());
        }
    }
}
