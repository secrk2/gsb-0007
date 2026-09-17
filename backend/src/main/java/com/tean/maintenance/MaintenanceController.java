package com.tean.maintenance;

import com.tean.auth.LoginUser;
import com.tean.auth.UserContext;
import com.tean.common.ApiResponse;
import com.tean.device.DeviceEntity;
import com.tean.device.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 维保记录与离线同步。
 * 幂等：clientId 唯一约束，重复提交/离线恢复重放均合并为同一记录。
 */
@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {
    private final MaintenanceRecordRepository recordRepository;
    private final DeviceService deviceService;

    public record RecordItem(@NotBlank String clientId,
                             @NotNull Long deviceId,
                             @NotBlank String content,
                             @NotNull LocalDateTime happenedAt,
                             boolean offline) {}

    public record SyncResult(String clientId, String status, Long recordId) {}

    @PostMapping
    public ApiResponse<SyncResult> create(@Valid @RequestBody RecordItem item) {
        return ApiResponse.ok(saveOne(UserContext.require(), item));
    }

    /** 离线恢复批量合并：逐条幂等落库，返回每条的处理结果 */
    @PostMapping("/sync")
    public ApiResponse<List<SyncResult>> sync(@Valid @RequestBody List<RecordItem> items) {
        LoginUser user = UserContext.require();
        List<SyncResult> results = new ArrayList<>();
        for (RecordItem item : items) {
            results.add(saveOne(user, item));
        }
        return ApiResponse.ok(results);
    }

    @GetMapping
    public ApiResponse<List<MaintenanceRecord>> list(@RequestParam Long deviceId) {
        deviceService.requireVisible(UserContext.require(), deviceId);
        return ApiResponse.ok(recordRepository.findByDeviceIdOrderByHappenedAtDesc(deviceId));
    }

    private SyncResult saveOne(LoginUser user, RecordItem item) {
        DeviceEntity device = deviceService.requireVisible(user, item.deviceId());

        var existing = recordRepository.findByClientId(item.clientId());
        if (existing.isPresent()) {
            return new SyncResult(item.clientId(), "DUPLICATE", existing.get().getId());
        }
        MaintenanceRecord record = new MaintenanceRecord();
        record.setClientId(item.clientId());
        record.setDeviceId(device.getId());
        record.setOrgId(device.getOrgId());
        record.setMaintainerId(user.getId());
        record.setMaintainerName(user.getName());
        record.setContent(item.content());
        record.setHappenedAt(item.happenedAt());
        record.setOffline(item.offline());
        try {
            MaintenanceRecord saved = recordRepository.saveAndFlush(record);
            return new SyncResult(item.clientId(), "CREATED", saved.getId());
        } catch (DataIntegrityViolationException e) {
            // 并发重复提交：唯一约束兜底，按已合并处理
            Long id = recordRepository.findByClientId(item.clientId())
                    .map(MaintenanceRecord::getId).orElse(null);
            return new SyncResult(item.clientId(), "DUPLICATE", id);
        }
    }
}
