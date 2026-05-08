package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.system.dto.OrgQuery;
import com.yx.lab.modules.system.dto.OrgSaveCommand;
import com.yx.lab.modules.system.entity.LabOrg;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabOrgMapper;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.vo.LabOrgVO;
import com.yx.lab.modules.system.vo.OrgOptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 机构管理服务。
 */
@Service
@RequiredArgsConstructor
public class OrgManagementService {

    private final LabOrgMapper labOrgMapper;

    private final LabUserMapper labUserMapper;

    /**
     * 分页查询机构列表。
     *
     * @param query 查询条件
     * @return 机构分页结果
     */
    public PageResult<LabOrgVO> page(OrgQuery query) {
        Page<LabOrg> page = labOrgMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabOrg>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(LabOrg::getOrgCode, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabOrg::getOrgName, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabOrg::getParentName, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabOrg::getOrgType, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(LabOrg::getRemark, StrUtil.trim(query.getKeyword())))
                        .eq(query.getStatus() != null, LabOrg::getStatus, query.getStatus())
                        .orderByAsc(LabOrg::getOrgCode)
                        .orderByDesc(LabOrg::getCreatedTime));
        List<LabOrgVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取机构详情。
     *
     * @param id 机构ID
     * @return 机构详情
     */
    public LabOrgVO detail(Long id) {
        return toVO(requireOrg(id));
    }

    /**
     * 获取启用机构选项。
     *
     * @return 机构下拉选项
     */
    public List<OrgOptionVO> options() {
        return labOrgMapper.selectList(new LambdaQueryWrapper<LabOrg>()
                        .eq(LabOrg::getStatus, 1)
                        .orderByAsc(LabOrg::getOrgCode))
                .stream()
                .map(this::toOption)
                .collect(Collectors.toList());
    }

    /**
     * 新增机构。
     *
     * @param command 机构保存命令
     */
    public void save(OrgSaveCommand command) {
        validateOrgCodeUnique(StrUtil.trim(command.getOrgCode()), null);
        validateOrgNameUnique(StrUtil.trim(command.getOrgName()), null);

        LabOrg entity = new LabOrg();
        applyCommand(entity, command, null);
        labOrgMapper.insert(entity);
    }

    /**
     * 更新机构。
     *
     * @param id 机构ID
     * @param command 机构保存命令
     */
    public void update(Long id, OrgSaveCommand command) {
        LabOrg entity = requireOrg(id);
        validateOrgCodeUnique(StrUtil.trim(command.getOrgCode()), id);
        validateOrgNameUnique(StrUtil.trim(command.getOrgName()), id);
        applyCommand(entity, command, id);
        labOrgMapper.updateById(entity);
    }

    /**
     * 删除机构。
     *
     * @param id 机构ID
     */
    public void delete(Long id) {
        LabOrg entity = requireOrg(id);
        Long childCount = labOrgMapper.selectCount(new LambdaQueryWrapper<LabOrg>()
                .eq(LabOrg::getParentId, entity.getId()));
        if (childCount != null && childCount > 0L) {
            throw new BusinessException("当前机构存在下级机构，不能删除");
        }
        Long userCount = labUserMapper.selectCount(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getOrgId, entity.getId()));
        if (userCount != null && userCount > 0L) {
            throw new BusinessException("当前机构已被用户使用，不能删除");
        }
        labOrgMapper.deleteById(entity.getId());
    }

    /**
     * 校验机构是否存在且已启用。
     *
     * @param orgId 机构ID
     */
    public LabOrg validateOrgUsable(Long orgId) {
        LabOrg org = requireOrg(orgId);
        if (org.getStatus() == null || org.getStatus() != 1) {
            throw new BusinessException("当前机构已停用，不能绑定到用户");
        }
        return org;
    }

    private LabOrg requireOrg(Long id) {
        LabOrg entity = labOrgMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("机构不存在");
        }
        return entity;
    }

    private void validateOrgCodeUnique(String orgCode, Long excludeId) {
        Long count = labOrgMapper.selectCount(new LambdaQueryWrapper<LabOrg>()
                .eq(LabOrg::getOrgCode, orgCode)
                .ne(excludeId != null, LabOrg::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("机构编码已存在");
        }
    }

    private void validateOrgNameUnique(String orgName, Long excludeId) {
        Long count = labOrgMapper.selectCount(new LambdaQueryWrapper<LabOrg>()
                .eq(LabOrg::getOrgName, orgName)
                .ne(excludeId != null, LabOrg::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("机构名称已存在");
        }
    }

    private void applyCommand(LabOrg entity, OrgSaveCommand command, Long currentId) {
        LabOrg parentOrg = null;
        if (command.getParentId() != null) {
            if (currentId != null && currentId.equals(command.getParentId())) {
                throw new BusinessException("上级机构不能选择自己");
            }
            parentOrg = requireOrg(command.getParentId());
        }

        entity.setOrgCode(StrUtil.trim(command.getOrgCode()));
        entity.setOrgName(StrUtil.trim(command.getOrgName()));
        entity.setParentId(parentOrg == null ? null : parentOrg.getId());
        entity.setParentName(parentOrg == null ? null : parentOrg.getOrgName());
        entity.setOrgType(StrUtil.trim(command.getOrgType()));
        entity.setStatus(command.getStatus());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private LabOrgVO toVO(LabOrg entity) {
        LabOrgVO vo = new LabOrgVO();
        vo.setId(entity.getId());
        vo.setOrgCode(entity.getOrgCode());
        vo.setOrgName(entity.getOrgName());
        vo.setParentId(entity.getParentId());
        vo.setParentName(entity.getParentName());
        vo.setOrgType(entity.getOrgType());
        vo.setStatus(entity.getStatus());
        vo.setMemberCount(labUserMapper.selectCount(new LambdaQueryWrapper<LabUser>()
                .eq(LabUser::getOrgId, entity.getId())));
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private OrgOptionVO toOption(LabOrg entity) {
        OrgOptionVO vo = new OrgOptionVO();
        vo.setId(entity.getId());
        vo.setOrgCode(entity.getOrgCode());
        vo.setOrgName(entity.getOrgName());
        return vo;
    }
}
