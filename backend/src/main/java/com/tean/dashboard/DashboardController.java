package com.tean.dashboard;

import com.tean.common.ApiResponse;
import com.tean.dashboard.DashboardDtos.Summary;
import com.tean.security.AuthUser;
import com.tean.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<Summary> summary() {
        AuthUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(dashboardService.summary(user));
    }
}
