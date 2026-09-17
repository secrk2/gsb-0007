package com.tean.maintenance;

import com.tean.common.BizException;
import com.tean.device.Device;
import com.tean.device.DeviceRepository;
import com.tean.maintenance.MaintenanceDtos.*;
import com.tean.security.AuthUser;
import com.tean.user.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 维保记录：离线同步入口。以 recordNo 为幂等键合并，恢复网络后重传不产生重复记录。
 */
@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository recordRepository;
    private final DeviceRepository deviceRepository;

    public MaintenanceService(MaintenanceRecordRepository recordRepository,
                              DeviceRepository deviceRepository) {
        this.recordRepository = recordRepository;
        this.deviceRepository = deviceRepository;
    }

    public List<MaintenanceItem> listByDevice(AuthUser user, Long deviceId) {
        Device device = mustGetDevice(deviceId);
        checkScope(user, device);
        return recordRepository.findByDeviceIdOrderByCreatedAtDesc(deviceId).stream()
                .map(r -> MaintenanceItem.of(r, device.getName()))
                .toList();
    }

    /**
     * 批量同步（离线队列恢复后调用）。逐条幂等：recordNo 已存在则返回 DUPLICATE，不重复写入。
     */
    @Transactional
    public SyncResult sync(AuthUser user, SyncRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw BizException.badRequest("同步列表为空");
        }
        if (request.items().size() > 100) {
            throw BizException.badRequest("单批最多同步 100 条，请分批提交");
        }
        List<SyncResultItem> results = new ArrayList<>();
        int created = 0;
        for (SyncItem item : request.items()) {
            SyncResultItem r = syncOne(user, item);
            if ("CREATED".equals(r.status())) {
                created++;
            }
            results.add(r);
        }
        return new SyncResult(results, created, results.size() - created);
    }

    private SyncResultItem syncOne(AuthUser user, SyncItem item) {
        if (item.recordNo() == null || item.recordNo().isBlank() || item.recordNo().length() > 64) {
            throw BizException.badRequest("recordNo 必填且不超过 64 字符（客户端生成的幂等键）");
        }
        if (item.content() == null || item.content().isBlank() || item.content().length() > 500) {
            throw BizException.badRequest("维保内容必填且不超过 500 字");
        }
        Device device = mustGetDevice(item.deviceId());
        checkScope(user, device);

        // 幂等合并：已存在则直接返回原记录
        Optional<MaintenanceRecord> existing = recordRepository.findByRecordNo(item.recordNo());
        if (existing.isPresent()) {
            return new SyncResultItem(item.recordNo(), "DUPLICATE", existing.get().getId());
        }

        MaintenanceRecord record = new MaintenanceRecord();
        record.setRecordNo(item.recordNo());
        record.setDeviceId(device.getId());
        record.setMaintainerId(user.getId());
        record.setMaintainerName(user.getRealName());
        record.setContent(item.content().trim());
        record.setOffline(Boolean.TRUE.equals(item.offline()));
        record.setClientCreatedAt(parseClientTime(item.clientCreatedAt()));
        try {
            MaintenanceRecord saved = recordRepository.saveAndFlush(record);
            return new SyncResultItem(item.recordNo(), "CREATED", saved.getId());
        } catch (DataIntegrityViolationException e) {
            // 并发/重试撞唯一键：按已存在处理，保证幂等
            Long serverId = recordRepository.findByRecordNo(item.recordNo())
                    .map(MaintenanceRecord::getId).orElse(null);
            return new SyncResultItem(item.recordNo(), "DUPLICATE", serverId);
        }
    }

    private Device mustGetDevice(Long deviceId) {
        if (deviceId == null) {
            throw BizException.badRequest("缺少设备 id");
        }
        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> BizException.notFound("设备不存在或已被删除"));
    }

    private void checkScope(AuthUser user, Device device) {
        if (user.getRole() == Role.SUPERVISOR) {
            throw BizException.forbidden("监察员只读，不能登记维保记录");
        }
        if (user.isOrgScoped() && !Objects.equals(device.getOrgId(), user.getOrgId())) {
            throw BizException.forbidden("无权为其他使用单位的设备登记维保记录");
        }
    }

    private LocalDateTime parseClientTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }
}
