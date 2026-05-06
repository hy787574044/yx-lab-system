package com.yx.lab.modules.system.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.system.dto.LoginRequest;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.vo.LoginVO;
import com.yx.lab.modules.system.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final LabUserMapper labUserMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final LabSecurityProperties securityProperties;

    public LoginVO login(LoginRequest request) {
        LabUser user = labUserMapper.selectOne(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getUsername, request.getUsername())
                .last("limit 1"));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户不存在或已停用");
        }
        String encryptPassword = SecureUtil.sha256(request.getPassword());
        if (!encryptPassword.equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = IdUtil.fastSimpleUUID();
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setRealName(user.getRealName());
        currentUser.setRoleCode(user.getRoleCode());
        stringRedisTemplate.opsForValue().set(
                "lab:token:" + token,
                JSONUtil.toJsonStr(currentUser),
                securityProperties.getTokenExpireHours(),
                TimeUnit.HOURS);
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .roleCode(user.getRoleCode())
                .build();
    }

    public UserProfileVO me() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException("请先登录");
        }
        return UserProfileVO.builder()
                .userId(currentUser.getUserId())
                .username(currentUser.getUsername())
                .realName(currentUser.getRealName())
                .roleCode(currentUser.getRoleCode())
                .build();
    }
}
