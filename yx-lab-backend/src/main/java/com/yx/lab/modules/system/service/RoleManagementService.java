package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.system.dto.RoleQuery;
import com.yx.lab.modules.system.dto.RoleSaveCommand;
import com.yx.lab.modules.system.entity.LabRole;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabRoleMapper;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.vo.LabRoleVO;
import com.yx.lab.modules.system.vo.RoleOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务。
 */
@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final LabRoleMapper labRoleMapper;

    private final LabUserMapper labUserMapper;

    /**
     * 分页查询角色列表。
     *
     * @param query 查询条件
     * @return 角色分页结果
     */
    public PageResult<LabRoleVO> page(RoleQuery query) {
        Page<LabRole> page = labRoleMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabRole>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabRole::getRoleCode, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabRole::getRoleName, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabRole::getRoleScope, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabRole::getRemark, StrUtil.trim(query.getKeyword())))
                        .eq(query.getStatus() != null, LabRole::getStatus, query.getStatus())
                        .orderByAsc(LabRole::getRoleCode)
                        .orderByDesc(LabRole::getCreatedTime));
        List<LabRoleVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取角色详情。
     *
     * @param id 角色ID
     * @return 角色详情
     */
    public LabRoleVO detail(Long id) {
        return toVO(requireRole(id));
    }

    /**
     * 获取启用角色下拉选项。
     *
     * @return 角色选项列表
     */
    public List<RoleOptionVO> options() {
        return labRoleMapper.selectList(new LambdaQueryWrapper<LabRole>()
                        .eq(LabRole::getStatus, 1)
                        .orderByAsc(LabRole::getRoleCode))
                .stream()
                .map(this::toOption)
                .collect(Collectors.toList());
    }

    /**
     * 新增角色。
     *
     * @param command 角色保存命令
     */
    public void save(RoleSaveCommand command) {
        validateRoleCodeUnique(StrUtil.trim(command.getRoleCode()), null);
        validateRoleNameUnique(StrUtil.trim(command.getRoleName()), null);

        LabRole entity = new LabRole();
        applyCommand(entity, command);
        labRoleMapper.insert(entity);
    }

    /**
     * 更新角色。
     *
     * @param id 角色ID
     * @param command 角色保存命令
     */
    public void update(Long id, RoleSaveCommand command) {
        LabRole entity = requireRole(id);
        validateRoleCodeUnique(StrUtil.trim(command.getRoleCode()), id);
        validateRoleNameUnique(StrUtil.trim(command.getRoleName()), id);
        applyCommand(entity, command);
        labRoleMapper.updateById(entity);
    }

    /**
     * 删除角色。
     *
     * @param id 角色ID
     */
    public void delete(Long id) {
        LabRole entity = requireRole(id);
        Long userCount = labUserMapper.selectCount(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getRoleCode, entity.getRoleCode()));
        if (userCount != null && userCount > 0L) {
            throw new BusinessException("当前角色已被用户使用，不能删除");
        }
        labRoleMapper.deleteById(entity.getId());
    }

    /**
     * 校验角色编码是否存在且已启用。
     *
     * @param roleCode 角色编码
     */
    public void validateRoleUsable(String roleCode) {
        LabRole role = labRoleMapper.selectOne(new LambdaQueryWrapper<LabRole>()
                .eq(LabRole::getRoleCode, StrUtil.trim(roleCode))
                .last("limit 1"));
        if (role == null) {
            throw new BusinessException("角色编码不存在");
        }
        if (role.getStatus() == null || role.getStatus() != 1) {
            throw new BusinessException("当前角色已停用，不能绑定到用户");
        }
    }

    private LabRole requireRole(Long id) {
        LabRole entity = labRoleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("角色不存在");
        }
        return entity;
    }

    private void validateRoleCodeUnique(String roleCode, Long excludeId) {
        Long count = labRoleMapper.selectCount(new LambdaQueryWrapper<LabRole>()
                .eq(LabRole::getRoleCode, roleCode)
                .ne(excludeId != null, LabRole::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("角色编码已存在");
        }
    }

    private void validateRoleNameUnique(String roleName, Long excludeId) {
        Long count = labRoleMapper.selectCount(new LambdaQueryWrapper<LabRole>()
                .eq(LabRole::getRoleName, roleName)
                .ne(excludeId != null, LabRole::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("角色名称已存在");
        }
    }

    private void applyCommand(LabRole entity, RoleSaveCommand command) {
        entity.setRoleCode(StrUtil.trim(command.getRoleCode()));
        entity.setRoleName(StrUtil.trim(command.getRoleName()));
        entity.setRoleScope(StrUtil.trim(command.getRoleScope()));
        entity.setStatus(command.getStatus());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private LabRoleVO toVO(LabRole entity) {
        LabRoleVO vo = new LabRoleVO();
        vo.setId(entity.getId());
        vo.setRoleCode(entity.getRoleCode());
        vo.setRoleName(entity.getRoleName());
        vo.setRoleScope(entity.getRoleScope());
        vo.setStatus(entity.getStatus());
        vo.setUserCount(labUserMapper.selectCount(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getRoleCode, entity.getRoleCode())));
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private RoleOptionVO toOption(LabRole entity) {
        RoleOptionVO vo = new RoleOptionVO();
        vo.setId(entity.getId());
        vo.setRoleCode(entity.getRoleCode());
        vo.setRoleName(entity.getRoleName());
        return vo;
    }
}
