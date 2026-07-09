package com.yx.lab.modules.gis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThirdPartyGisTokenScheduler {

    private final ThirdPartyGisService thirdPartyGisService;

    @Scheduled(
            fixedDelayString = "${lab.gis.refresh-fixed-delay-ms:300000}",
            initialDelayString = "${lab.gis.refresh-initial-delay-ms:10000}"
    )
    public void refreshTokenCache() {
        try {
            thirdPartyGisService.refreshTokenCacheIfNecessary();
        } catch (Exception exception) {
            log.warn("Refresh third-party GIS token cache failed. error={}", exception.getMessage(), exception);
        }
    }
}
