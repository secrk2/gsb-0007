package com.tean.device;

import com.tean.common.ApiResponse;
import com.tean.common.PageResult;
import com.tean.device.DeviceDtos.*;
import com.tean.security.AuthUser;
import com.tean.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 设备档案接口。
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** 元数据：状态下拉与类型下拉 */
    @GetMapping("/meta")
    public ApiResponse<Map<String, Object>> meta() {
        List<StatusOption> statuses = Arrays.stream(DeviceStatus.values())
                .map(s -> new StatusOption(s.name(), s.getLabel()))
                .toList();
        List<StatusOption> types = Arrays.stream(DeviceType.values())
                .map(t -> new StatusOption(t.name(), t.getLabel()))
                .toList();
        return ApiResponse.ok(Map.of("statuses", statuses, "types", types));
    }

    @GetMapping
    public ApiResponse<PageResult<DeviceView>> page(@RequestParam(required = false) DeviceStatus status,
                                                    @RequestParam(required = false) DeviceType type,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(deviceService.page(user, status, type, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceDetail> detail(@PathVariable Long id) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(deviceService.detail(user, id));
    }

    /** 状态流转：非法回退/跳变返回 409 并说明原因 */
    @PostMapping("/{id}/transition")
    public ApiResponse<DeviceDetail> transition(@PathVariable Long id,
                                                @RequestBody TransitionRequest request) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(deviceService.transition(user, id, request));
    }

    /** 精确地址：仅监察员，需理由 + 二次确认，全程留痕 */
    @PostMapping("/{id}/address/reveal")
    public ApiResponse<RevealResult> revealAddress(@PathVariable Long id,
                                                   @RequestBody RevealRequest request,
                                                   HttpServletRequest http) {
        AuthUser user = SecurityUtils.currentUser();
        String ip = clientIp(http);
        return ApiResponse.ok(deviceService.revealAddress(user, id, request, ip));
    }

    /** 地址查看留痕（仅监察员） */
    @GetMapping("/{id}/address/reveals")
    public ApiResponse<List<RevealLogItem>> revealLogs(@PathVariable Long id) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(deviceService.revealLogs(user, id));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
