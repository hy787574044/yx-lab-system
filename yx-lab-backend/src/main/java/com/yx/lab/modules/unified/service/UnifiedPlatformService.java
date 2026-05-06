package com.yx.lab.modules.unified.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.modules.unified.config.UnifiedPlatformProperties;
import com.yx.lab.modules.unified.dto.UnifiedAccessTokenRequest;
import com.yx.lab.modules.unified.dto.UnifiedAuthorizeRequest;
import com.yx.lab.modules.unified.dto.UnifiedRefreshTokenRequest;
import com.yx.lab.modules.unified.dto.UnifiedRevokeTokenRequest;
import com.yx.lab.modules.unified.dto.UnifiedRoleUserRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserIdRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserInfoByOpenIdRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserJobNoRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserQueryRequest;
import com.yx.lab.modules.unified.vo.UnifiedAuthorizeCodeVO;
import com.yx.lab.modules.unified.vo.UnifiedMenuItemVO;
import com.yx.lab.modules.unified.vo.UnifiedMenuVO;
import com.yx.lab.modules.unified.vo.UnifiedOperationVO;
import com.yx.lab.modules.unified.vo.UnifiedTokenVO;
import com.yx.lab.modules.unified.vo.UnifiedUserInfoVO;
import com.yx.lab.modules.unified.vo.UnifiedUserListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UnifiedPlatformService {

    private static final String AUTHORIZE_PATH = "/HData/DevApi/proxy/oauth2/authorize";
    private static final String TOKEN_PATH = "/HData/DevApi/proxy/oauth2/token";
    private static final String REFRESH_PATH = "/HData/DevApi/proxy/oauth2/refresh";
    private static final String REVOKE_PATH = "/HData/DevApi/proxy/oauth2/revoke";
    private static final String USERINFO_BY_OPEN_ID_PATH = "/HData/DevApi/proxy/oauth2/userinfo";
    private static final String QUERY_USER_INFO_PATH = "/HData/DevApi/proxy/openapi/user/info/query";
    private static final String USER_INFO_BY_ID_PATH = "/HData/DevApi/proxy/openapi/user/info";
    private static final String USER_INFO_BY_JOB_NO_PATH = "/HData/DevApi/proxy/openapi/user/infoByJobNO";
    private static final String USER_LIST_BY_ROLE_ID_PATH = "/HData/DevApi/proxy/openapi/user/listByRoleld";
    private static final String USER_MENU_PATH = "/HData/DevApi/proxy/openapi/user/menu";

    private final UnifiedPlatformProperties properties;

    private final ObjectMapper objectMapper;

    public UnifiedAuthorizeCodeVO authorize(UnifiedAuthorizeRequest request) {
        RemoteResponse response = invokeGet(AUTHORIZE_PATH, buildAuthorizeParams(request));
        UnifiedAuthorizeCodeVO result = new UnifiedAuthorizeCodeVO();
        result.setCode(pickText(response, "code", "authCode", "authorizationCode"));
        result.setRawBody(response.getBody());
        result.setRaw(response.getRoot());
        return result;
    }

    public UnifiedTokenVO getAccessToken(UnifiedAccessTokenRequest request) {
        RemoteResponse response = invokeGet(TOKEN_PATH, buildAccessTokenParams(request));
        return buildToken(response);
    }

    public UnifiedTokenVO refreshAccessToken(UnifiedRefreshTokenRequest request) {
        RemoteResponse response = invokeGet(REFRESH_PATH, buildRefreshTokenParams(request));
        return buildToken(response);
    }

    public UnifiedOperationVO revokeAccessToken(UnifiedRevokeTokenRequest request) {
        RemoteResponse response = invokeGet(REVOKE_PATH, buildRevokeParams(request));
        UnifiedOperationVO result = new UnifiedOperationVO();
        result.setSuccess(detectSuccess(response));
        result.setMessage(pickText(response, "message", "msg", "resultMsg"));
        result.setRawBody(response.getBody());
        result.setRaw(response.getRoot());
        return result;
    }

    public UnifiedUserInfoVO getUserInfoByOpenId(UnifiedUserInfoByOpenIdRequest request) {
        RemoteResponse response = invokeGet(USERINFO_BY_OPEN_ID_PATH, buildOpenIdParams(request));
        return buildUserInfo(response, chooseBestNode(response));
    }

    public UnifiedUserInfoVO queryUserInfo(UnifiedUserQueryRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("jobNo", request.getJobNo());
        params.put("mobile", request.getMobile());
        RemoteResponse response = invokeGet(QUERY_USER_INFO_PATH, params);
        return buildUserInfo(response, chooseBestNode(response));
    }

    public UnifiedUserInfoVO getUserInfoById(UnifiedUserIdRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", request.getId());
        RemoteResponse response = invokeGet(USER_INFO_BY_ID_PATH, params);
        return buildUserInfo(response, chooseBestNode(response));
    }

    public UnifiedUserInfoVO getUserInfoByJobNo(UnifiedUserJobNoRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("jobNO", request.getJobNo());
        RemoteResponse response = invokeGet(USER_INFO_BY_JOB_NO_PATH, params);
        return buildUserInfo(response, chooseBestNode(response));
    }

    public UnifiedUserListVO listUsersByRoleId(UnifiedRoleUserRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("roleld", request.getRoleId());
        RemoteResponse response = invokeGet(USER_LIST_BY_ROLE_ID_PATH, params);
        UnifiedUserListVO result = new UnifiedUserListVO();
        JsonNode arrayNode = findArrayNode(response, chooseBestNode(response), "list", "rows", "records", "data", "users");
        if (arrayNode != null) {
            for (JsonNode item : arrayNode) {
                result.getUsers().add(buildUserInfo(response, item));
            }
        }
        result.setRawBody(response.getBody());
        result.setRaw(response.getRoot());
        return result;
    }

    public UnifiedMenuVO getUserMenus(UnifiedUserIdRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", request.getId());
        RemoteResponse response = invokeGet(USER_MENU_PATH, params);
        UnifiedMenuVO result = new UnifiedMenuVO();
        JsonNode arrayNode = findArrayNode(response, chooseBestNode(response), "list", "rows", "records", "data", "menus");
        if (arrayNode != null) {
            for (JsonNode item : arrayNode) {
                result.getMenus().add(buildMenuItem(item));
            }
        }
        result.setRawBody(response.getBody());
        result.setRaw(response.getRoot());
        return result;
    }

    private Map<String, Object> buildAuthorizeParams(UnifiedAuthorizeRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("response_type", firstNonBlank(request.getResponseType(), properties.getResponseType(), "code"));
        params.put("client_id", firstNonBlank(request.getClientId(), properties.getClientId()));
        params.put("scope", firstNonBlank(request.getScope(), properties.getScope()));
        return params;
    }

    private Map<String, Object> buildAccessTokenParams(UnifiedAccessTokenRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("grant_type", firstNonBlank(request.getGrantType(), "authorization_code"));
        params.put("client_id", firstNonBlank(request.getClientId(), properties.getClientId()));
        params.put("client_secret", firstNonBlank(request.getClientSecret(), properties.getClientSecret()));
        params.put("code", request.getCode());
        return params;
    }

    private Map<String, Object> buildRefreshTokenParams(UnifiedRefreshTokenRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("grant_type", firstNonBlank(request.getGrantType(), "refresh_token"));
        params.put("client_id", firstNonBlank(request.getClientId(), properties.getClientId()));
        params.put("client_secret", firstNonBlank(request.getClientSecret(), properties.getClientSecret()));
        params.put("refresh_token", request.getRefreshToken());
        return params;
    }

    private Map<String, Object> buildRevokeParams(UnifiedRevokeTokenRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("client_id", firstNonBlank(request.getClientId(), properties.getClientId()));
        params.put("client_secret", firstNonBlank(request.getClientSecret(), properties.getClientSecret()));
        params.put("access_token", request.getAccessToken());
        return params;
    }

    private Map<String, Object> buildOpenIdParams(UnifiedUserInfoByOpenIdRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("access_token", request.getAccessToken());
        params.put("open_id", request.getOpenId());
        return params;
    }

    private UnifiedTokenVO buildToken(RemoteResponse response) {
        UnifiedTokenVO result = new UnifiedTokenVO();
        result.setAccessToken(pickText(response, "access_token", "accessToken", "token"));
        result.setRefreshToken(pickText(response, "refresh_token", "refreshToken"));
        result.setTokenType(pickText(response, "token_type", "tokenType"));
        result.setExpiresIn(pickLong(response, "expires_in", "expiresIn"));
        result.setScope(pickText(response, "scope"));
        result.setOpenId(pickText(response, "open_id", "openId"));
        result.setRawBody(response.getBody());
        result.setRaw(response.getRoot());
        return result;
    }

    private UnifiedUserInfoVO buildUserInfo(RemoteResponse response, JsonNode node) {
        JsonNode source = node == null ? response.getRoot() : node;
        UnifiedUserInfoVO result = new UnifiedUserInfoVO();
        result.setUserId(pickText(source, response, "userId", "user_id", "id"));
        result.setOpenId(pickText(source, response, "openId", "open_id"));
        result.setUsername(pickText(source, response, "username", "userName", "account"));
        result.setRealName(pickText(source, response, "realName", "real_name", "name", "nickName"));
        result.setJobNo(pickText(source, response, "jobNo", "jobNO", "job_no", "workNo"));
        result.setMobile(pickText(source, response, "mobile", "phone", "tel"));
        result.setDepartmentName(pickText(source, response, "departmentName", "deptName", "orgName"));
        result.setRawBody(response.getBody());
        result.setRaw(source);
        return result;
    }

    private UnifiedMenuItemVO buildMenuItem(JsonNode node) {
        UnifiedMenuItemVO result = new UnifiedMenuItemVO();
        result.setMenuId(pickText(node, "menuId", "id"));
        result.setMenuName(pickText(node, "menuName", "name", "title"));
        result.setMenuCode(pickText(node, "menuCode", "code", "permission"));
        result.setPath(pickText(node, "path", "url"));
        result.setComponent(pickText(node, "component"));
        JsonNode children = findArrayNode(node, "children", "childList", "menus", "items");
        if (children != null) {
            for (JsonNode child : children) {
                result.getChildren().add(buildMenuItem(child));
            }
        }
        result.setRaw(node);
        return result;
    }

    private RemoteResponse invokeGet(String path, Map<String, Object> params) {
        validateBaseConfig();
        String url = buildUrl(path);
        HttpRequest request = HttpRequest.get(url).timeout(properties.getTimeout());
        applyHeaders(request);
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() != null && StrUtil.isNotBlank(String.valueOf(entry.getValue()))) {
                request.form(entry.getKey(), entry.getValue());
            }
        }
        try (HttpResponse response = request.execute()) {
            String body = response.body();
            if (response.getStatus() >= 400) {
                throw new BusinessException("unified platform request failed: HTTP " + response.getStatus() + ", body=" + body);
            }
            if (StrUtil.isBlank(body)) {
                throw new BusinessException("unified platform returned empty body");
            }
            JsonNode root = objectMapper.readTree(body);
            return new RemoteResponse(body, root, unwrapData(root));
        } catch (IOException exception) {
            throw new BusinessException("failed to parse unified platform response: " + exception.getMessage());
        } catch (Exception exception) {
            if (exception instanceof BusinessException) {
                throw (BusinessException) exception;
            }
            throw new BusinessException("failed to call unified platform: " + exception.getMessage());
        }
    }

    private void applyHeaders(HttpRequest request) {
        if (StrUtil.isNotBlank(properties.getApiToken())) {
            request.header("HDataApiToken", properties.getApiToken());
        }
        for (Map.Entry<String, String> entry : properties.getHeaders().entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                request.header(entry.getKey(), entry.getValue());
            }
        }
    }

    private void validateBaseConfig() {
        if (StrUtil.isBlank(properties.getBaseUrl())) {
            throw new BusinessException("lab.unified.base-url is not configured");
        }
    }

    private String buildUrl(String path) {
        String baseUrl = StrUtil.removeSuffix(properties.getBaseUrl(), "/");
        return baseUrl + path;
    }

    private JsonNode unwrapData(JsonNode root) {
        if (root == null) {
            return null;
        }
        for (String field : new String[]{"data", "result", "content", "body"}) {
            JsonNode node = root.get(field);
            if (node != null && !node.isNull()) {
                return node;
            }
        }
        return root;
    }

    private JsonNode chooseBestNode(RemoteResponse response) {
        if (response.getData() != null && !response.getData().isNull()) {
            return response.getData();
        }
        return response.getRoot();
    }

    private boolean detectSuccess(RemoteResponse response) {
        JsonNode source = response.getRoot();
        JsonNode successNode = source.get("success");
        if (successNode != null && successNode.isBoolean()) {
            return successNode.asBoolean();
        }
        JsonNode codeNode = source.get("code");
        if (codeNode != null) {
            if (codeNode.isInt() || codeNode.isLong()) {
                return codeNode.asLong() == 0L || codeNode.asLong() == 200L;
            }
            String text = codeNode.asText();
            return "0".equals(text) || "200".equals(text) || "success".equalsIgnoreCase(text);
        }
        return true;
    }

    private String pickText(RemoteResponse response, String... fields) {
        String value = pickText(response.getData(), fields);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return pickText(response.getRoot(), fields);
    }

    private String pickText(JsonNode preferredNode, RemoteResponse response, String... fields) {
        String value = pickText(preferredNode, fields);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return pickText(response.getRoot(), fields);
    }

    private String pickText(JsonNode node, String... fields) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = pickText(item, fields);
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
            return null;
        }
        for (String field : fields) {
            JsonNode child = node.get(field);
            if (child != null && !child.isNull() && !child.isContainerNode()) {
                String value = child.asText();
                if (StrUtil.isNotBlank(value)) {
                    return value;
                }
            }
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String value = pickText(entry.getValue(), fields);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private Long pickLong(RemoteResponse response, String... fields) {
        Long value = pickLong(response.getData(), fields);
        if (value != null) {
            return value;
        }
        return pickLong(response.getRoot(), fields);
    }

    private Long pickLong(JsonNode node, String... fields) {
        String text = pickText(node, fields);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private JsonNode findArrayNode(RemoteResponse response, JsonNode preferredNode, String... fieldNames) {
        JsonNode arrayNode = findArrayNode(preferredNode, fieldNames);
        if (arrayNode != null) {
            return arrayNode;
        }
        return findArrayNode(response.getRoot(), fieldNames);
    }

    private JsonNode findArrayNode(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            return node;
        }
        for (String fieldName : fieldNames) {
            JsonNode child = node.get(fieldName);
            if (child != null && child.isArray()) {
                return child;
            }
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            JsonNode child = entry.getValue();
            if (child != null) {
                if (child.isArray()) {
                    return child;
                }
                JsonNode nested = findArrayNode(child, fieldNames);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static class RemoteResponse {

        private final String body;

        private final JsonNode root;

        private final JsonNode data;

        private RemoteResponse(String body, JsonNode root, JsonNode data) {
            this.body = body;
            this.root = root;
            this.data = data;
        }

        public String getBody() {
            return body;
        }

        public JsonNode getRoot() {
            return root;
        }

        public JsonNode getData() {
            return data;
        }
    }
}
