package com.yx.lab.modules.system.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.system.dto.LoginRequest;
import com.yx.lab.modules.system.dto.PasswordChangeCommand;
import com.yx.lab.modules.system.dto.UserProfileUpdateCommand;
import com.yx.lab.modules.system.entity.LabLoginLog;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabLoginLogMapper;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.vo.LoginVO;
import com.yx.lab.modules.system.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private static final String LOGIN_STATUS_SUCCESS = "SUCCESS";
    private static final String LOGIN_STATUS_FAILED = "FAILED";

    private final LabUserMapper labUserMapper;

    private final LabLoginLogMapper labLoginLogMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final LabSecurityProperties securityProperties;

    /**
     * 默认按 PC 端执行登录。
     *
     * @param request 登录请求
     * @return 登录结果
     */
    public LoginVO login(LoginRequest request) {
        return login(request, "PC");
    }

    /**
     * 按指定渠道执行登录。
     *
     * @param request 登录请求
     * @param loginChannel 登录渠道
     * @return 登录结果
     */
    public LoginVO login(LoginRequest request, String loginChannel) {
        String username = request == null ? null : request.getUsername();
        LabUser user = labUserMapper.selectOne(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getUsername, username)
                .last("limit 1"));
        if (user == null) {
            recordFailedLogin(null, username, loginChannel, "用户不存在");
            throw new BusinessException("用户不存在或已停用");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordFailedLogin(user, username, loginChannel, "账号已停用");
            throw new BusinessException("用户不存在或已停用");
        }

        String encryptPassword = SecureUtil.sha256(request.getPassword());
        if (!encryptPassword.equals(user.getPassword())) {
            recordFailedLogin(user, username, loginChannel, "密码错误");
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
        saveLoginLog(user, username, loginChannel, LOGIN_STATUS_SUCCESS, "登录成功");
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .orgId(user.getOrgId())
                .orgName(user.getOrgName())
                .roleCode(user.getRoleCode())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    /**
     * 获取当前登录人信息。
     *
     * @return 当前登录人信息
     */
    public UserProfileVO me() {
        return buildProfile(requireCurrentUserEntity());
    }

    /**
     * 修改当前登录人基础资料。
     *
     * @param command 资料修改命令
     * @param token 当前登录令牌
     * @return 修改后的当前登录人信息
     */
    public UserProfileVO updateProfile(UserProfileUpdateCommand command, String token) {
        LabUser user = requireCurrentUserEntity();
        user.setRealName(StrUtil.trim(command.getRealName()));
        user.setPhone(StrUtil.trim(command.getPhone()));
        user.setAvatarUrl(StrUtil.trim(command.getAvatarUrl()));
        labUserMapper.updateById(user);
        refreshTokenUser(token, user);
        return buildProfile(user);
    }

    /**
     * 修改当前登录人密码。
     *
     * @param command 密码修改命令
     */
    public void changePassword(PasswordChangeCommand command) {
        LabUser user = requireCurrentUserEntity();
        String oldPassword = StrUtil.trim(command.getOldPassword());
        if (!SecureUtil.sha256(oldPassword).equals(user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        String newPassword = StrUtil.trim(command.getNewPassword());
        if (StrUtil.isBlank(newPassword)) {
            throw new BusinessException("新密码不能为空");
        }
        if (SecureUtil.sha256(newPassword).equals(user.getPassword())) {
            throw new BusinessException("新密码不能与原密码一致");
        }
        user.setPassword(SecureUtil.sha256(newPassword));
        labUserMapper.updateById(user);
    }

    /**
     * 退出登录，清理当前令牌。
     *
     * @param token 当前登录令牌
     */
    public void logout(String token) {
        if (StrUtil.isNotBlank(token)) {
            stringRedisTemplate.delete("lab:token:" + token);
        }
    }

    private LabUser requireCurrentUserEntity() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("请先登录");
        }
        LabUser user = labUserMapper.selectById(currentUser.getUserId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户不存在或已停用");
        }
        return user;
    }

    private UserProfileVO buildProfile(LabUser user) {
        return UserProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .orgId(user.getOrgId())
                .orgName(user.getOrgName())
                .roleCode(user.getRoleCode())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .build();
    }

    private void refreshTokenUser(String token, LabUser user) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        String redisKey = "lab:token:" + token;
        Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setRealName(user.getRealName());
        currentUser.setRoleCode(user.getRoleCode());
        if (ttl != null && ttl > 0L) {
            stringRedisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(currentUser), ttl, TimeUnit.SECONDS);
        } else {
            stringRedisTemplate.opsForValue().set(
                    redisKey,
                    JSONUtil.toJsonStr(currentUser),
                    securityProperties.getTokenExpireHours(),
                    TimeUnit.HOURS);
        }
    }

    /**
     * 记录失败登录日志并继续抛出业务异常。
     *
     * @param user 命中的用户，可能为空
     * @param username 请求用户名
     * @param loginChannel 登录渠道
     * @param remark 失败原因
     */
    private void recordFailedLogin(LabUser user, String username, String loginChannel, String remark) {
        saveLoginLog(user, username, loginChannel, LOGIN_STATUS_FAILED, remark);
    }

    /**
     * 保存登录日志。
     *
     * @param user 命中的用户，失败场景可能为空
     * @param username 请求用户名
     * @param loginChannel 登录渠道
     * @param loginStatus 登录状态
     * @param remark 备注说明
     */
    private void saveLoginLog(LabUser user,
                              String username,
                              String loginChannel,
                              String loginStatus,
                              String remark) {
        try {
            LabLoginLog loginLog = new LabLoginLog();
            loginLog.setUserId(user == null ? null : user.getId());
            loginLog.setUsername(resolveUsername(user, username));
            loginLog.setRealName(user == null ? null : user.getRealName());
            loginLog.setRoleCode(user == null ? null : user.getRoleCode());
            loginLog.setLoginChannel(loginChannel);
            loginLog.setLoginStatus(loginStatus);
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setRemark(remark);
            labLoginLogMapper.insert(loginLog);
        } catch (Exception ex) {
            log.warn("保存登录日志失败，username={}", resolveUsername(user, username), ex);
        }
    }

    /**
     * 解析日志中应记录的登录账号。
     *
     * @param user 命中的用户
     * @param username 请求用户名
     * @return 最终记录账号
     */
    private String resolveUsername(LabUser user, String username) {
        if (user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            return user.getUsername().trim();
        }
        return username == null ? "" : username.trim();
    }
}
