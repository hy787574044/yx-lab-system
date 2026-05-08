package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.system.dto.UserQuery;
import com.yx.lab.modules.system.dto.UserSaveCommand;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.vo.LabUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理服务。
 */
@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final LabUserMapper labUserMapper;

    /**
     * 分页查询用户列表。
     *
     * @param query 查询条件
     * @return 用户分页结果
     */
    public PageResult<LabUserVO> page(UserQuery query) {
        Page<LabUser> page = labUserMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabUser>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabUser::getUsername, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabUser::getRealName, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabUser::getPhone, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabUser::getRoleCode, StrUtil.trim(query.getKeyword())))
                        .eq(query.getStatus() != null, LabUser::getStatus, query.getStatus())
                        .orderByDesc(LabUser::getCreatedTime));
        List<LabUserVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取用户详情。
     *
     * @param id 用户ID
     * @return 用户详情
     */
    public LabUserVO detail(Long id) {
        return toVO(requireUser(id));
    }

    /**
     * 新增用户。
     *
     * @param command 用户保存命令
     */
    public void save(UserSaveCommand command) {
        String password = StrUtil.trim(command.getPassword());
        if (StrUtil.isBlank(password)) {
            throw new BusinessException("新增用户时密码不能为空");
        }
        validateUsernameUnique(StrUtil.trim(command.getUsername()), null);

        LabUser entity = new LabUser();
        applyCommand(entity, command);
        entity.setPassword(SecureUtil.sha256(password));
        labUserMapper.insert(entity);
    }

    /**
     * 更新用户。
     *
     * @param id 用户ID
     * @param command 用户保存命令
     */
    public void update(Long id, UserSaveCommand command) {
        LabUser entity = requireUser(id);
        validateUsernameUnique(StrUtil.trim(command.getUsername()), id);
        applyCommand(entity, command);
        String password = StrUtil.trim(command.getPassword());
        if (StrUtil.isNotBlank(password)) {
            entity.setPassword(SecureUtil.sha256(password));
        }
        labUserMapper.updateById(entity);
    }

    /**
     * 删除用户。
     *
     * @param id 用户ID
     */
    public void delete(Long id) {
        LabUser entity = requireUser(id);
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser != null && currentUser.getUserId() != null && currentUser.getUserId().equals(entity.getId())) {
            throw new BusinessException("不允许删除当前登录用户");
        }
        labUserMapper.deleteById(entity.getId());
    }

    private LabUser requireUser(Long id) {
        LabUser entity = labUserMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("用户不存在");
        }
        return entity;
    }

    private void validateUsernameUnique(String username, Long excludeId) {
        Long count = labUserMapper.selectCount(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getUsername, username)
                .ne(excludeId != null, LabUser::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("用户名已存在");
        }
    }

    private void applyCommand(LabUser entity, UserSaveCommand command) {
        entity.setUsername(StrUtil.trim(command.getUsername()));
        entity.setRealName(StrUtil.trim(command.getRealName()));
        entity.setRoleCode(StrUtil.trim(command.getRoleCode()));
        entity.setPhone(StrUtil.trim(command.getPhone()));
        entity.setStatus(command.getStatus());
    }

    private LabUserVO toVO(LabUser entity) {
        LabUserVO vo = new LabUserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setRealName(entity.getRealName());
        vo.setRoleCode(entity.getRoleCode());
        vo.setPhone(entity.getPhone());
        vo.setStatus(entity.getStatus());
        vo.setCreatedName(entity.getCreatedName());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedName(entity.getUpdatedName());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }
}
