package com.yx.lab.modules.system.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.config.LabSecurityProperties;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.PermissionService;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.system.dto.EmbedLoginRequest;
import com.yx.lab.modules.system.dto.LoginRequest;
import com.yx.lab.modules.system.dto.PasswordChangeCommand;
import com.yx.lab.modules.system.dto.UserProfileUpdateCommand;
import com.yx.lab.modules.system.entity.LabLoginLog;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabLoginLogMapper;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.unified.dto.UnifiedUserIdRequest;
import com.yx.lab.modules.unified.dto.UnifiedUserJobNoRequest;
import com.yx.lab.modules.unified.service.UnifiedPlatformService;
import com.yx.lab.modules.unified.vo.UnifiedUserInfoVO;
import com.yx.lab.modules.system.vo.CaptchaVO;
import com.yx.lab.modules.system.vo.LoginVO;
import com.yx.lab.modules.system.vo.UserProfileVO;
import com.yx.lab.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
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
    private static final String CAPTCHA_REDIS_PREFIX = "lab:captcha:";
    private static final String CAPTCHA_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int CAPTCHA_EXPIRE_MINUTES = 3;
    private static final Random CAPTCHA_RANDOM = new Random();

    private final LabUserMapper labUserMapper;

    private final LabLoginLogMapper labLoginLogMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final LabSecurityProperties securityProperties;

    private final PermissionService permissionService;

    private final StorageService storageService;

    private final UnifiedPlatformService unifiedPlatformService;

    private void validateCaptcha(LoginRequest request) {
        if (request == null || StrUtil.isBlank(request.getCaptchaId()) || StrUtil.isBlank(request.getCaptchaCode())) {
            throw new BusinessException("请输入验证码");
        }
        String captchaKey = CAPTCHA_REDIS_PREFIX + request.getCaptchaId();
        String cachedCode = stringRedisTemplate.opsForValue().get(captchaKey);
        stringRedisTemplate.delete(captchaKey);
        if (StrUtil.isBlank(cachedCode)) {
            throw new BusinessException("验证码已过期，请刷新后重试");
        }
        String inputCode = StrUtil.trim(request.getCaptchaCode()).toUpperCase(Locale.ROOT);
        if (!cachedCode.equalsIgnoreCase(inputCode)) {
            throw new BusinessException("验证码错误");
        }
    }

    private String createCaptchaCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            builder.append(CAPTCHA_CHARS.charAt(CAPTCHA_RANDOM.nextInt(CAPTCHA_CHARS.length())));
        }
        return builder.toString();
    }

    private String createCaptchaImage(String code) {
        int width = 132;
        int height = 42;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(237, 247, 255));
            graphics.fillRect(0, 0, width, height);
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(
                        120 + CAPTCHA_RANDOM.nextInt(80),
                        150 + CAPTCHA_RANDOM.nextInt(70),
                        180 + CAPTCHA_RANDOM.nextInt(60)));
                graphics.drawLine(
                        CAPTCHA_RANDOM.nextInt(width),
                        CAPTCHA_RANDOM.nextInt(height),
                        CAPTCHA_RANDOM.nextInt(width),
                        CAPTCHA_RANDOM.nextInt(height));
            }
            graphics.setFont(new Font("Arial", Font.BOLD, 26));
            for (int i = 0; i < code.length(); i++) {
                graphics.setColor(new Color(
                        20 + CAPTCHA_RANDOM.nextInt(40),
                        80 + CAPTCHA_RANDOM.nextInt(80),
                        120 + CAPTCHA_RANDOM.nextInt(80)));
                graphics.drawString(String.valueOf(code.charAt(i)), 18 + i * 26, 29 + CAPTCHA_RANDOM.nextInt(5));
            }
        } finally {
            graphics.dispose();
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new BusinessException("验证码生成失败");
        }
    }

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
     * 生成登录验证码，并把答案短期存入缓存供登录校验。
     *
     * @return 验证码标识和图片
     */
    public CaptchaVO createCaptcha() {
        String code = createCaptchaCode();
        String captchaId = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_REDIS_PREFIX + captchaId,
                code,
                CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
        return new CaptchaVO(captchaId, createCaptchaImage(code));
    }

    /**
     * 按指定渠道执行登录。
     *
     * @param request 登录请求
     * @param loginChannel 登录渠道
     * @return 登录结果
     */
    public LoginVO login(LoginRequest request, String loginChannel) {
        if ("PC".equalsIgnoreCase(loginChannel)) {
            validateCaptcha(request);
        }
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

        saveLoginLog(user, username, loginChannel, LOGIN_STATUS_SUCCESS, "登录成功");
        return issueLoginToken(user);
    }

    /**
     * 获取当前登录人信息。
     *
     * @return 当前登录人信息
     */
    /**
     * 第三方嵌入登录：将外部入口身份兑换为本系统登录令牌。
     *
     * @param request 嵌入登录参数
     * @return 本系统登录结果
     */
    public LoginVO embedLogin(EmbedLoginRequest request) {
        if (request == null || StrUtil.isBlank(request.getToken())) {
            throw new BusinessException("第三方令牌不能为空");
        }
        LabUser user = resolveEmbedUser(request);
        if (user == null) {
            throw new BusinessException("未找到对应的系统账号，请联系管理员绑定账号");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户不存在或已停用");
        }
        saveLoginLog(user, user.getUsername(), resolveEmbedLoginChannel(request), LOGIN_STATUS_SUCCESS, "第三方嵌入登录成功");
        return issueLoginToken(user);
    }

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
        user.setAvatarUrl(storageService.toFullUrl(command.getAvatarUrl()));
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
                .permissionCodes(permissionService.resolvePermissions(user.getRoleCode()))
                .dataScope(permissionService.resolveDataScope(user.getRoleCode()))
                .phone(user.getPhone())
                .avatarUrl(storageService.toFullUrl(user.getAvatarUrl()))
                .status(user.getStatus())
                .build();
    }

    private LabUser resolveEmbedUser(EmbedLoginRequest request) {
        UnifiedUserInfoVO unifiedUser = resolveUnifiedUser(request);
        String jobNo = firstNonBlank(unifiedUser == null ? null : unifiedUser.getJobNo(), request.getJobNo());
        String username = firstNonBlank(unifiedUser == null ? null : unifiedUser.getUsername(), request.getUsername(), jobNo);
        LabUser user = selectActiveUserByUsername(username);
        if (user != null) {
            return user;
        }
        String realName = unifiedUser == null ? null : unifiedUser.getRealName();
        if (StrUtil.isNotBlank(realName)) {
            return labUserMapper.selectOne(new LambdaQueryWrapper<LabUser>()
                    .eq(LabUser::getRealName, StrUtil.trim(realName))
                    .eq(LabUser::getStatus, 1)
                    .last("limit 1"));
        }
        return null;
    }

    private UnifiedUserInfoVO resolveUnifiedUser(EmbedLoginRequest request) {
        try {
            if (StrUtil.isNotBlank(request.getUserId())) {
                UnifiedUserIdRequest userIdRequest = new UnifiedUserIdRequest();
                userIdRequest.setId(StrUtil.trim(request.getUserId()));
                return unifiedPlatformService.getUserInfoById(userIdRequest);
            }
            if (StrUtil.isNotBlank(request.getJobNo())) {
                UnifiedUserJobNoRequest jobNoRequest = new UnifiedUserJobNoRequest();
                jobNoRequest.setJobNo(StrUtil.trim(request.getJobNo()));
                return unifiedPlatformService.getUserInfoByJobNo(jobNoRequest);
            }
        } catch (BusinessException exception) {
            log.warn("第三方嵌入登录查询统一平台用户失败，userId="
                    + request.getUserId()
                    + ", jobNo="
                    + request.getJobNo(),
                    exception);
        }
        return null;
    }

    private LabUser selectActiveUserByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return labUserMapper.selectOne(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getUsername, StrUtil.trim(username))
                .eq(LabUser::getStatus, 1)
                .last("limit 1"));
    }

    private LoginVO issueLoginToken(LabUser user) {
        String token = IdUtil.fastSimpleUUID();
        CurrentUser currentUser = buildCurrentUser(user);
        stringRedisTemplate.opsForValue().set(
                "lab:token:" + token,
                JSONUtil.toJsonStr(currentUser),
                securityProperties.getTokenExpireHours(),
                TimeUnit.HOURS);
        return buildLoginVO(token, user, currentUser);
    }

    private CurrentUser buildCurrentUser(LabUser user) {
        CurrentUser currentUser = new CurrentUser();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setRealName(user.getRealName());
        currentUser.setRoleCode(user.getRoleCode());
        currentUser.setPermissionCodes(permissionService.resolvePermissions(user.getRoleCode()));
        currentUser.setDataScope(permissionService.resolveDataScope(user.getRoleCode()));
        return currentUser;
    }

    private LoginVO buildLoginVO(String token, LabUser user, CurrentUser currentUser) {
        return LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .orgId(user.getOrgId())
                .orgName(user.getOrgName())
                .roleCode(user.getRoleCode())
                .permissionCodes(currentUser.getPermissionCodes())
                .dataScope(currentUser.getDataScope())
                .phone(user.getPhone())
                .avatarUrl(storageService.toFullUrl(user.getAvatarUrl()))
                .build();
    }

    private String resolveEmbedLoginChannel(EmbedLoginRequest request) {
        String channelType = request == null ? null : StrUtil.trim(request.getChannelType());
        return StrUtil.isBlank(channelType) ? "EMBED" : "EMBED-" + channelType;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return null;
    }

    private void refreshTokenUser(String token, LabUser user) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        String redisKey = "lab:token:" + token;
        Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        CurrentUser currentUser = buildCurrentUser(user);
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
