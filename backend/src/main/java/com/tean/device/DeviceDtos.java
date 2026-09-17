package com.tean.device;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备模块对外 DTO。注意：任何 DTO 都不包含 exactAddress，精确地址只通过 reveal 接口单独下发。
 */
public final class DeviceDtos {

    private DeviceDtos() {
    }

    /** 列表/看板视图：位置已脱敏为「区域 · 编号」 */
    public record DeviceView(
            Long id,
            String deviceCode,
            String name,
            DeviceType type,
            String typeLabel,
            DeviceStatus status,
            String statusLabel,
            Long orgId,
            String orgName,
            String locationMasked,
            LocalDate nextInspectionDate,
            Long version) {

        public static DeviceView of(Device d, String orgName) {
            return new DeviceView(
                    d.getId(), d.getDeviceCode(), d.getName(),
                    d.getType(), d.getType().getLabel(),
                    d.getStatus(), d.getStatus().getLabel(),
                    d.getOrgId(), orgName,
                    d.getRegion() + " · " + d.getMaskCode(),
                    d.getNextInspectionDate(),
                    d.getVersion());
        }
    }

    public record StatusOption(String value, String label) {
    }

    public record StatusLogItem(
            String fromStatus, String fromLabel,
            String toStatus, String toLabel,
            String operatorName, String reason,
            LocalDateTime createdAt) {
    }

    public record DeviceDetail(
            DeviceView device,
            String model,
            LocalDate installDate,
            LocalDateTime createdAt,
            List<StatusOption> allowedTransitions,
            boolean canReveal,
            List<StatusLogItem> statusLogs) {
    }

    public record TransitionRequest(String toStatus, String reason) {
    }

    public record RevealRequest(String reason, Boolean confirm) {
    }

    public record RevealResult(String exactAddress, String operator, String revealedAt, String notice) {
    }

    public record RevealLogItem(String realName, String username, String reason, String ip, LocalDateTime createdAt) {
    }
}
