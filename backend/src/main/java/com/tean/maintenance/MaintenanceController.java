package com.tean.maintenance;

import com.tean.common.ApiResponse;
import com.tean.maintenance.MaintenanceDtos.*;
import com.tean.security.AuthUser;
import com.tean.security.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ApiResponse<List<MaintenanceItem>> listByDevice(@RequestParam Long deviceId) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(maintenanceService.listByDevice(user, deviceId));
    }

    /** 离线队列批量同步：幂等合并，重复 recordNo 不会产生重复记录 */
    @PostMapping("/sync")
    public ApiResponse<SyncResult> sync(@RequestBody SyncRequest request) {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(maintenanceService.sync(user, request));
    }
}
