package com.yx.lab.modules.gis.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.gis.service.ThirdPartyGisService;
import com.yx.lab.modules.gis.vo.ThirdPartyGisTokenVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/third-party-gis")
@RequiredArgsConstructor
public class ThirdPartyGisController {

    private final ThirdPartyGisService thirdPartyGisService;

    @GetMapping("/token")
    public ApiResponse<ThirdPartyGisTokenVO> getToken() {
        return ApiResponse.success(thirdPartyGisService.getToken());
    }

    @RequestMapping(value = "/attribute-space-query", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<JsonNode> attributeSpaceQuery() {
        return ApiResponse.success(thirdPartyGisService.attributeSpaceQuery());
    }

    @PostMapping("/token/refresh")
    public ApiResponse<ThirdPartyGisTokenVO> refreshToken() {
        return ApiResponse.success(thirdPartyGisService.forceRefreshToken());
    }
}
