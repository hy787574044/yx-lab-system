package com.yx.lab.common.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private final LabSecurityProperties securityProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader(securityProperties.getTokenHeader());
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith(securityProperties.getTokenPrefix())) {
            throw new BusinessException("请先登录");
        }
        String token = authHeader.substring(securityProperties.getTokenPrefix().length()).trim();
        String cacheValue = stringRedisTemplate.opsForValue().get("lab:token:" + token);
        if (StrUtil.isBlank(cacheValue)) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        SecurityContext.setCurrentUser(JSONUtil.toBean(cacheValue, CurrentUser.class));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContext.clear();
    }
}
