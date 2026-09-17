package com.tean.device;

import com.tean.auth.LoginUser;
import com.tean.auth.UserContext;
import com.tean.common.ApiResponse;
import com.tean.device.DeviceDtos.DeviceView;
import com.tean.device.DeviceDtos.RevealRequest;
import com.tean.device.DeviceDtos.RevealResult;
import com.tean.device.DeviceDtos.TransitionRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @GetMapping
    public ApiResponse<Page<DeviceView>> list(@RequestParam(required = false) DeviceStatus status,
                                              @RequestParam(required = false) DeviceType type,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(deviceService.list(UserContext.require(), status, type, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceView> detail(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.detail(UserContext.require(), id));
    }

    @PostMapping("/{id}/transition")
    public ApiResponse<DeviceView> transit(@PathVariable Long id, @RequestBody TransitionRequest req) {
        LoginUser user = UserContext.require();
        if (req.to() == null) {
            throw com.tean.common.BizException.badRequest("缺少目标状态");
        }
        return ApiResponse.ok(deviceService.transit(user, id, req.to(), req.reason()));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<DeviceStatusHistory>> history(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.history(UserContext.require(), id));
    }

    @PostMapping("/{id}/address/reveal")
    public ApiResponse<RevealResult> reveal(@PathVariable Long id,
                                            @RequestBody RevealRequest req,
                                            HttpServletRequest request) {
        return ApiResponse.ok(deviceService.revealAddress(
                UserContext.require(), id, req.reason(), req.confirm(), request.getRemoteAddr()));
    }

    @GetMapping("/{id}/address/reveals")
    public ApiResponse<List<AddressRevealLog>> revealLogs(@PathVariable Long id) {
        return ApiResponse.ok(deviceService.revealLogs(UserContext.require(), id));
    }
}
