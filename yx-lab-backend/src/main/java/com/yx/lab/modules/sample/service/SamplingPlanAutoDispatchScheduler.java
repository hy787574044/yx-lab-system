package com.yx.lab.modules.sample.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SamplingPlanAutoDispatchScheduler {

    private final SamplingPlanService samplingPlanService;

    @Scheduled(cron = "0 * * * * ?")
    public void autoDispatchPlans() {
        samplingPlanService.autoDispatchDuePlans(LocalDateTime.now());
    }
}
