package com.yourid.api.analytics.controller;

import com.yourid.api.analytics.dto.DashboardResponse;
import com.yourid.api.analytics.dto.TrackEventRequest;
import com.yourid.api.analytics.service.AnalyticsService;
import com.yourid.api.common.response.ApiResponse;
import com.yourid.api.common.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/track")
    public ApiResponse<Void> track(@Valid @RequestBody TrackEventRequest request, HttpServletRequest httpRequest) {
        analyticsService.track(request, httpRequest.getRemoteAddr());
        return ApiResponse.ok(null);
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(analyticsService.getDashboard(principal.getId()));
    }
}
