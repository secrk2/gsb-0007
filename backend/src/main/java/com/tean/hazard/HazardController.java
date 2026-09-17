package com.tean.hazard;

import com.tean.common.ApiResponse;
import com.tean.hazard.HazardDtos.HazardItem;
import com.tean.security.AuthUser;
import com.tean.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hazards")
public class HazardController {

    private final HazardService hazardService;

    public HazardController(HazardService hazardService) {
        this.hazardService = hazardService;
    }

    @GetMapping
    public ApiResponse<List<HazardItem>> list(@RequestParam(required = false) HazardStatus status) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(hazardService.list(user, status));
    }

    @GetMapping("/device/{deviceId}")
    public ApiResponse<List<HazardItem>> listByDevice(@PathVariable Long deviceId) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(hazardService.listByDevice(user, deviceId));
    }

    @PostMapping("/{id}/resolve")
    public ApiResponse<HazardItem> resolve(@PathVariable Long id) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(hazardService.resolve(user, id));
    }
}
