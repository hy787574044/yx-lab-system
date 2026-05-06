package com.yx.lab.modules.unified.controller;

import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.modules.unified.dto.UnifiedAccessTokenRequest;
import com.yx.lab.modules.unified.dto.UnifiedAuthorizeRequest;
import com.yx.lab.modules.unified.dto.UnifiedRefreshTokenRequest;
import com.yx.lab.modules.unified.dto.UnifiedRevokeTokenRequest;
import com.yx.lab.modules.unified.dto.UnifiedRoleUserRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserIdRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserInfoByOpenIdRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserJobNoRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserQueryRequest;
import com.yx.lab.modules.unified.service.UnifiedPlatformService;
import com.yx.lab.modules.unified.vo.UnifiedAuthorizeCodeVO;
import com.yx.lab.modules.unified.vo.UnifiedMenuVO;
import com.yx.lab.modules.unified.vo.UnifiedOperationVO;
import com.yx.lab.modules.unified.vo.UnifiedTokenVO;
import com.yx.lab.modules.unified.vo.UnifiedUserInfoVO;
import com.yx.lab.modules.unified.vo.UnifiedUserListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/unified")
@RequiredArgsConstructor
public class UnifiedPlatformController {

    private final UnifiedPlatformService unifiedPlatformService;

    @GetMapping("/oauth/authorize")
    public ApiResponse<UnifiedAuthorizeCodeVO> authorize(@Valid UnifiedAuthorizeRequest request) {
        return ApiResponse.success(unifiedPlatformService.authorize(request));
    }

    @GetMapping("/oauth/token")
    public ApiResponse<UnifiedTokenVO> getAccessToken(@Valid UnifiedAccessTokenRequest request) {
        return ApiResponse.success(unifiedPlatformService.getAccessToken(request));
    }

    @GetMapping("/oauth/refresh")
    public ApiResponse<UnifiedTokenVO> refreshAccessToken(@Valid UnifiedRefreshTokenRequest request) {
        return ApiResponse.success(unifiedPlatformService.refreshAccessToken(request));
    }

    @GetMapping("/oauth/revoke")
    public ApiResponse<UnifiedOperationVO> revokeAccessToken(@Valid UnifiedRevokeTokenRequest request) {
        return ApiResponse.success(unifiedPlatformService.revokeAccessToken(request));
    }

    @GetMapping("/oauth/userinfo")
    public ApiResponse<UnifiedUserInfoVO> getUserInfoByOpenId(@Valid UnifiedUserInfoByOpenIdRequest request) {
        return ApiResponse.success(unifiedPlatformService.getUserInfoByOpenId(request));
    }

    @GetMapping("/users/query")
    public ApiResponse<UnifiedUserInfoVO> queryUserInfo(@Valid UnifiedUserQueryRequest request) {
        return ApiResponse.success(unifiedPlatformService.queryUserInfo(request));
    }

    @GetMapping("/users/by-id")
    public ApiResponse<UnifiedUserInfoVO> getUserInfoById(@Valid UnifiedUserIdRequest request) {
        return ApiResponse.success(unifiedPlatformService.getUserInfoById(request));
    }

    @GetMapping("/users/by-job-no")
    public ApiResponse<UnifiedUserInfoVO> getUserInfoByJobNo(@Valid UnifiedUserJobNoRequest request) {
        return ApiResponse.success(unifiedPlatformService.getUserInfoByJobNo(request));
    }

    @GetMapping("/users/by-role-id")
    public ApiResponse<UnifiedUserListVO> listUsersByRoleId(@Valid UnifiedRoleUserRequest request) {
        return ApiResponse.success(unifiedPlatformService.listUsersByRoleId(request));
    }

    @GetMapping("/users/menus")
    public ApiResponse<UnifiedMenuVO> getUserMenus(@Valid UnifiedUserIdRequest request) {
        return ApiResponse.success(unifiedPlatformService.getUserMenus(request));
    }
}
