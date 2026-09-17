package com.tean.maintenance;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 维保模块 DTO。
 */
public final class MaintenanceDtos {

    private MaintenanceDtos() {
    }

    public record MaintenanceItem(
            Long id,
            String recordNo,
            Long deviceId,
            String deviceName,
            String maintainerName,
            String content,
            Boolean offline,
            LocalDateTime clientCreatedAt,
            LocalDateTime createdAt) {

        public static MaintenanceItem of(MaintenanceRecord r, String deviceName) {
            return new MaintenanceItem(
                    r.getId(), r.getRecordNo(), r.getDeviceId(), deviceName,
                    r.getMaintainerName(), r.getContent(), r.getOffline(),
                    r.getClientCreatedAt(), r.getCreatedAt());
        }
    }

    /** 离线同步单条：recordNo 为幂等键 */
    public record SyncItem(String recordNo, Long deviceId, String content,
                           String clientCreatedAt, Boolean offline) {
    }

    public record SyncRequest(List<SyncItem> items) {
    }

    /** CREATED=新入库；DUPLICATE=幂等命中，未重复写入 */
    public record SyncResultItem(String recordNo, String status, Long serverId) {
    }

    public record SyncResult(List<SyncResultItem> results, int created, int duplicated) {
    }
}
