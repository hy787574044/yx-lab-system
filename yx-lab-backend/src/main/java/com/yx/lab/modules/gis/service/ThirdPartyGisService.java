package com.yx.lab.modules.gis.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.modules.gis.config.ThirdPartyGisProperties;
import com.yx.lab.modules.gis.model.ThirdPartyGisTokenCache;
import com.yx.lab.modules.gis.vo.ThirdPartyGisTokenVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThirdPartyGisService {

    private static final String TOKEN_PATH = "/api/oauth/token/shared_platform";
    private static final String REFRESH_TOKEN_PATH = "/api/oauth/refresh_token/shared_platform";
    private static final String ATTRIBUTE_SPACE_QUERY_PATH = "/api/7d725ff8";
    private static final String ATTRIBUTE_SPACE_QUERY_SERVICE_NAME_PARAM = "serviceName";
    private static final String ATTRIBUTE_SPACE_QUERY_SERVICE_NAME_VALUE = "yxgw";
    private static final String ATTRIBUTE_SPACE_QUERY_LAYER_ID_PARAM = "layerId";
    private static final String ATTRIBUTE_SPACE_QUERY_LAYER_ID_VALUE = "0-1";
    private static final String ATTRIBUTE_SPACE_QUERY_RESULT_RECORD_COUNT_PARAM = "resultRecordCount";
    private static final String ATTRIBUTE_SPACE_QUERY_RESULT_RECORD_COUNT_VALUE = "50";
    private static final String ATTRIBUTE_SPACE_QUERY_FORMAT_PARAM = "f";
    private static final String ATTRIBUTE_SPACE_QUERY_FORMAT_VALUE = "json";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final String GIS_ATTRIBUTES_FIELD = "attributes";
    private static final String GIS_MONITOR_POINT_CATEGORY_FIELD = "监测点类别";
    private static final String GIS_MONITOR_POINT_CATEGORY_VALUE = "在线监测点";
    private static final String GIS_MONITOR_POINT_SUBTYPE_FIELD = "子类";
    private static final String GIS_MONITOR_POINT_SUBTYPE_VALUE = "压力监测点";
    private static final String REDIS_TOKEN_KEY = "lab:third-party-gis:token";
    private static final long DEFAULT_EXPIRES_IN_SECONDS = 3600L;

    private final ThirdPartyGisProperties properties;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final AtomicReference<ThirdPartyGisTokenCache> localToken = new AtomicReference<>();

    public ThirdPartyGisTokenVO getToken() {
        ThirdPartyGisTokenCache memoryCache = localToken.get();
        if (isUsable(memoryCache)) {
            return buildVO(memoryCache, "memory");
        }

        ThirdPartyGisTokenCache redisCache = loadRedisToken();
        if (isUsable(redisCache)) {
            localToken.set(redisCache);
            return buildVO(redisCache, "redis");
        }

        return buildVO(refreshTokenCache(true), "remote");
    }

    public ThirdPartyGisTokenVO forceRefreshToken() {
        return buildVO(refreshTokenCache(true), "remote");
    }

    public JsonNode attributeSpaceQuery() {
        ThirdPartyGisTokenCache tokenCache = resolveUsableTokenCache();
        String url = buildUrlWithFixedQueryParams(ATTRIBUTE_SPACE_QUERY_PATH);
        long startTime = System.currentTimeMillis();
        log.info("Third-party GIS attribute-space-query request start, method=POST, url=" + url);
        HttpRequest request = HttpRequest.post(url)
                .timeout(properties.getTimeout())
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + tokenCache.getAccessToken())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .form(ATTRIBUTE_SPACE_QUERY_RESULT_RECORD_COUNT_PARAM, ATTRIBUTE_SPACE_QUERY_RESULT_RECORD_COUNT_VALUE)
                .form(ATTRIBUTE_SPACE_QUERY_FORMAT_PARAM, ATTRIBUTE_SPACE_QUERY_FORMAT_VALUE);

        try (HttpResponse response = request.execute()) {
            String body = response.body();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Third-party GIS attribute-space-query response, url="
                    + url
                    + ", status="
                    + response.getStatus()
                    + ", elapsed="
                    + elapsed
                    + "ms");
            if (response.getStatus() >= 400) {
                throw new BusinessException("第三方GIS属性和空间查询失败，HTTP状态码: " + response.getStatus());
            }
            if (StrUtil.isBlank(body)) {
                throw new BusinessException("第三方GIS属性和空间查询返回内容为空");
            }
            JsonNode root = objectMapper.readTree(body);
            RemoteResponse remoteResponse = new RemoteResponse(root, unwrapData(root));
            if (!detectSuccess(remoteResponse)) {
                throw new BusinessException(firstNonBlank(pickText(remoteResponse, "message", "msg"), "第三方GIS属性和空间查询失败"));
            }
            return filterPressureMonitoringPoints(root);
        } catch (IOException exception) {
            throw new BusinessException("解析第三方GIS属性和空间查询响应失败: " + exception.getMessage());
        } catch (Exception exception) {
            if (exception instanceof BusinessException) {
                throw (BusinessException) exception;
            }
            throw new BusinessException("调用第三方GIS属性和空间查询失败: " + exception.getMessage());
        }
    }

    private JsonNode filterPressureMonitoringPoints(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isArray()) {
            if (!containsGisAttributeObjects(node)) {
                return node.deepCopy();
            }
            ArrayNode filtered = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                if (matchesPressureMonitoringPoint(item)) {
                    filtered.add(item);
                }
            }
            return filtered;
        }
        if (!node.isObject()) {
            return node.deepCopy();
        }

        ObjectNode copy = objectMapper.createObjectNode();
        Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            copy.set(entry.getKey(), filterPressureMonitoringPoints(entry.getValue()));
        }
        return copy;
    }

    private boolean containsGisAttributeObjects(JsonNode arrayNode) {
        for (JsonNode item : arrayNode) {
            JsonNode attributes = item.get(GIS_ATTRIBUTES_FIELD);
            if (attributes != null && attributes.isObject()) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPressureMonitoringPoint(JsonNode item) {
        if (item == null || item.isNull()) {
            return false;
        }
        JsonNode attributes = item.get(GIS_ATTRIBUTES_FIELD);
        if (attributes == null || !attributes.isObject()) {
            return false;
        }
        return GIS_MONITOR_POINT_CATEGORY_VALUE.equals(pickText(attributes, GIS_MONITOR_POINT_CATEGORY_FIELD))
                && GIS_MONITOR_POINT_SUBTYPE_VALUE.equals(pickText(attributes, GIS_MONITOR_POINT_SUBTYPE_FIELD));
    }

    public void refreshTokenCacheIfNecessary() {
        ThirdPartyGisTokenCache cache = localToken.get();
        if (cache == null) {
            cache = loadRedisToken();
        }
        if (!shouldRefresh(cache)) {
            if (cache != null) {
                localToken.set(cache);
            }
            return;
        }
        refreshTokenCache(false);
    }

    private ThirdPartyGisTokenCache resolveUsableTokenCache() {
        ThirdPartyGisTokenCache memoryCache = localToken.get();
        if (isUsable(memoryCache)) {
            return memoryCache;
        }

        ThirdPartyGisTokenCache redisCache = loadRedisToken();
        if (isUsable(redisCache)) {
            localToken.set(redisCache);
            return redisCache;
        }

        return refreshTokenCache(true);
    }

    private synchronized ThirdPartyGisTokenCache refreshTokenCache(boolean force) {
        ThirdPartyGisTokenCache cache = localToken.get();
        if (cache == null) {
            cache = loadRedisToken();
        }
        if (!force && !shouldRefresh(cache)) {
            localToken.set(cache);
            return cache;
        }

        ThirdPartyGisTokenCache refreshed = tryRefreshRemoteToken(cache);
        if (refreshed == null) {
            refreshed = requestAccessToken();
        }
        saveToken(refreshed);
        return refreshed;
    }

    private ThirdPartyGisTokenCache tryRefreshRemoteToken(ThirdPartyGisTokenCache cache) {
        if (cache == null || StrUtil.isBlank(cache.getRefreshToken()) || StrUtil.isBlank(properties.getLoginName())) {
            return null;
        }

        Map<String, Object> form = new LinkedHashMap<>();
        form.put("loginName", properties.getLoginName());
        form.put("refreshToken", cache.getRefreshToken());
        try {
            return requestToken(REFRESH_TOKEN_PATH, form);
        } catch (Exception exception) {
            log.warn("Third-party GIS token refresh failed, fallback to auth. error={}", exception.getMessage(), exception);
            return null;
        }
    }

    private ThirdPartyGisTokenCache requestAccessToken() {
        validateBaseConfig();
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("appKey", properties.getAppKey());
        form.put("appSecret", properties.getAppSecret());
        return requestToken(TOKEN_PATH, form);
    }

    private ThirdPartyGisTokenCache requestToken(String path, Map<String, Object> form) {
        validateBaseConfig();
        String url = buildUrl(path);
        long startTime = System.currentTimeMillis();
        log.info("Third-party GIS request start, method=POST, url={}, timeout={}ms", url, properties.getTimeout());
        HttpRequest request = HttpRequest.post(url)
                .timeout(properties.getTimeout())
                .header("Content-Type", "application/x-www-form-urlencoded");
        for (Map.Entry<String, Object> entry : form.entrySet()) {
            if (entry.getValue() != null && StrUtil.isNotBlank(String.valueOf(entry.getValue()))) {
                request.form(entry.getKey(), entry.getValue());
            }
        }

        try (HttpResponse response = request.execute()) {
            String body = response.body();
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Third-party GIS response, url="
                    + url
                    + ", status="
                    + response.getStatus()
                    + ", elapsed="
                    + elapsed
                    + "ms");
            if (response.getStatus() >= 400) {
                throw new BusinessException("第三方GIS接口请求失败，HTTP状态码: " + response.getStatus());
            }
            if (StrUtil.isBlank(body)) {
                throw new BusinessException("第三方GIS接口返回内容为空");
            }
            JsonNode root = objectMapper.readTree(body);
            RemoteResponse remoteResponse = new RemoteResponse(root, unwrapData(root));
            if (!detectSuccess(remoteResponse)) {
                throw new BusinessException(firstNonBlank(pickText(remoteResponse, "message", "msg"), "第三方GIS鉴权失败"));
            }
            return buildTokenCache(remoteResponse);
        } catch (IOException exception) {
            throw new BusinessException("解析第三方GIS响应失败: " + exception.getMessage());
        } catch (Exception exception) {
            if (exception instanceof BusinessException) {
                throw (BusinessException) exception;
            }
            throw new BusinessException("调用第三方GIS接口失败: " + exception.getMessage());
        }
    }

    private ThirdPartyGisTokenCache buildTokenCache(RemoteResponse response) {
        String accessToken = pickText(response, "access_token", "accessToken", "token");
        if (StrUtil.isBlank(accessToken)) {
            throw new BusinessException("第三方GIS响应缺少access_token");
        }
        long expiresIn = firstPositive(pickLong(response, "expires_in", "expiresIn"), DEFAULT_EXPIRES_IN_SECONDS);
        long now = System.currentTimeMillis();

        ThirdPartyGisTokenCache cache = new ThirdPartyGisTokenCache();
        cache.setAccessToken(accessToken);
        cache.setTokenType(firstNonBlank(pickText(response, "token_type", "tokenType"), "Bearer"));
        cache.setExpiresIn(expiresIn);
        cache.setRefreshToken(pickText(response, "refresh_token", "refreshToken"));
        cache.setCachedAtMillis(now);
        cache.setExpireAtMillis(now + TimeUnit.SECONDS.toMillis(expiresIn));
        return cache;
    }

    private ThirdPartyGisTokenCache loadRedisToken() {
        String value;
        try {
            value = stringRedisTemplate.opsForValue().get(REDIS_TOKEN_KEY);
        } catch (Exception exception) {
            log.warn("Read third-party GIS token from Redis failed. error={}", exception.getMessage(), exception);
            return null;
        }
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, ThirdPartyGisTokenCache.class);
        } catch (Exception exception) {
            log.warn("Parse third-party GIS token cache failed. error={}", exception.getMessage(), exception);
            return null;
        }
    }

    private void saveToken(ThirdPartyGisTokenCache cache) {
        localToken.set(cache);
        long ttlSeconds = Math.max(1L, TimeUnit.MILLISECONDS.toSeconds(cache.getExpireAtMillis() - System.currentTimeMillis()));
        try {
            stringRedisTemplate.opsForValue().set(REDIS_TOKEN_KEY, objectMapper.writeValueAsString(cache), ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new BusinessException("写入第三方GIS token 缓存失败: " + exception.getMessage());
        }
    }

    private boolean isUsable(ThirdPartyGisTokenCache cache) {
        if (cache == null || StrUtil.isBlank(cache.getAccessToken()) || cache.getExpireAtMillis() == null) {
            return false;
        }
        long refreshBeforeMillis = TimeUnit.SECONDS.toMillis(Math.max(0L, properties.getRefreshBeforeExpireSeconds()));
        return cache.getExpireAtMillis() > System.currentTimeMillis() + refreshBeforeMillis;
    }

    private boolean shouldRefresh(ThirdPartyGisTokenCache cache) {
        return !isUsable(cache);
    }

    private ThirdPartyGisTokenVO buildVO(ThirdPartyGisTokenCache cache, String cacheSource) {
        ThirdPartyGisTokenVO vo = new ThirdPartyGisTokenVO();
        vo.setAccessToken(cache.getAccessToken());
        vo.setTokenType(cache.getTokenType());
        vo.setExpiresIn(cache.getExpiresIn());
        vo.setRefreshToken(cache.getRefreshToken());
        vo.setCachedAtMillis(cache.getCachedAtMillis());
        vo.setExpireAtMillis(cache.getExpireAtMillis());
        vo.setCacheSource(cacheSource);
        return vo;
    }

    private void validateBaseConfig() {
        if (StrUtil.isBlank(properties.getBaseUrl())) {
            throw new BusinessException("未配置第三方GIS地址 lab.gis.base-url");
        }
        if (StrUtil.isBlank(properties.getAppKey())) {
            throw new BusinessException("未配置第三方GIS appKey lab.gis.app-key");
        }
        if (StrUtil.isBlank(properties.getAppSecret())) {
            throw new BusinessException("未配置第三方GIS appSecret lab.gis.app-secret");
        }
    }

    private String buildUrl(String path) {
        return StrUtil.removeSuffix(properties.getBaseUrl(), "/") + path;
    }

    private String buildUrlWithFixedQueryParams(String path) {
        return buildUrl(path)
                + "?"
                + encodeQueryPart(ATTRIBUTE_SPACE_QUERY_SERVICE_NAME_PARAM)
                + "="
                + encodeQueryPart(ATTRIBUTE_SPACE_QUERY_SERVICE_NAME_VALUE)
                + "&"
                + encodeQueryPart(ATTRIBUTE_SPACE_QUERY_LAYER_ID_PARAM)
                + "="
                + encodeQueryPart(ATTRIBUTE_SPACE_QUERY_LAYER_ID_VALUE);
    }

    private String encodeQueryPart(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            return value == null ? "" : value;
        }
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

    private boolean detectSuccess(RemoteResponse response) {
        JsonNode root = response.getRoot();
        JsonNode successNode = root.get("success");
        if (successNode != null && successNode.isBoolean()) {
            return successNode.asBoolean();
        }
        JsonNode codeNode = root.get("code");
        if (codeNode != null) {
            if (codeNode.isNumber()) {
                return codeNode.asLong() == 0L || codeNode.asLong() == 200L;
            }
            String code = codeNode.asText();
            return "0".equals(code) || "200".equals(code) || "success".equalsIgnoreCase(code);
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

    private String pickText(JsonNode node, String... fields) {
        if (node == null || node.isNull()) {
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
        return null;
    }

    private Long pickLong(RemoteResponse response, String... fields) {
        String text = pickText(response, fields);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return Long.valueOf(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private long firstPositive(Long value, long defaultValue) {
        return value != null && value > 0 ? value : defaultValue;
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

        private final JsonNode root;

        private final JsonNode data;

        private RemoteResponse(JsonNode root, JsonNode data) {
            this.root = root;
            this.data = data;
        }

        public JsonNode getRoot() {
            return root;
        }

        public JsonNode getData() {
            return data;
        }
    }
}
