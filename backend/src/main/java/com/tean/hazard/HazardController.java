package com.tean.hazard;

import com.tean.auth.LoginUser;
import com.tean.auth.Role;
import com.tean.auth.UserContext;
import com.tean.common.ApiResponse;
import com.tean.common.BizException;
import com.tean.device.DeviceEntity;
import com.tean.device.DeviceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/hazards")
@RequiredArgsConstructor
public class HazardController {
    private final HazardRepository hazardRepository;
    private final DeviceService deviceService;

    public record CreateRequest(@NotNull Long deviceId, @NotBlank String title,
                                @NotNull HazardEntity.Level level, @NotNull LocalDate deadline) {}

    @GetMapping
    public ApiResponse<List<HazardEntity>> list(@RequestParam(required = false) Long deviceId) {
        LoginUser user = UserContext.require();
        if (deviceId != null) {
            deviceService.requireVisible(user, deviceId);
            return ApiResponse.ok(hazardRepository.findByDeviceId(deviceId));
        }
        if (user.getRole().isAgency()) {
            return ApiResponse.ok(hazardRepository.findAll());
        }
        // 使用单位/维保单位均按本单位归集
        return ApiResponse.ok(hazardRepository.findByOrgId(user.getOrgId()));
    }

    @PostMapping
    public ApiResponse<HazardEntity> create(@Valid @RequestBody CreateRequest req) {
        LoginUser user = UserContext.require();
        DeviceEntity device = deviceService.requireVisible(user, req.deviceId());
        HazardEntity hazard = new HazardEntity();
        hazard.setDeviceId(device.getId());
        hazard.setOrgId(device.getOrgId());
        hazard.setTitle(req.title());
        hazard.setLevel(req.level());
        hazard.setDeadline(req.deadline());
        return ApiResponse.ok(hazardRepository.save(hazard));
    }

    @PostMapping("/{id}/rectify")
    public ApiResponse<HazardEntity> rectify(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        HazardEntity hazard = hazardRepository.findById(id)
                .orElseThrow(() -> BizException.notFound("隐患不存在"));
        if (!user.getRole().isAgency() && !hazard.getOrgId().equals(user.getOrgId())) {
            throw BizException.forbidden("无权处理其他单位的隐患");
        }
        if (hazard.getStatus() == HazardEntity.Status.RECTIFIED) {
            return ApiResponse.ok(hazard);
        }
        hazard.setStatus(HazardEntity.Status.RECTIFIED);
        hazard.setRectifiedAt(LocalDateTime.now());
        return ApiResponse.ok(hazardRepository.save(hazard));
    }
}
