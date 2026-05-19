package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.system.dto.FlowConfigQuery;
import com.yx.lab.modules.system.dto.FlowConfigSaveCommand;
import com.yx.lab.modules.system.dto.FlowNodeCommand;
import com.yx.lab.modules.system.entity.LabFlowConfig;
import com.yx.lab.modules.system.entity.LabFlowNode;
import com.yx.lab.modules.system.entity.LabRole;
import com.yx.lab.modules.system.entity.LabUser;
import com.yx.lab.modules.system.mapper.LabFlowConfigMapper;
import com.yx.lab.modules.system.mapper.LabFlowNodeMapper;
import com.yx.lab.modules.system.mapper.LabRoleMapper;
import com.yx.lab.modules.system.mapper.LabUserMapper;
import com.yx.lab.modules.system.vo.FlowConfigOptionVO;
import com.yx.lab.modules.system.vo.FlowConfigVO;
import com.yx.lab.modules.system.vo.FlowNodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程配置管理服务。
 */
@Service
@RequiredArgsConstructor
public class FlowConfigManagementService {

    public static final String FLOW_TYPE_REVIEW = "REVIEW";
    public static final String FLOW_TYPE_PUBLISH = "PUBLISH";

    private final LabFlowConfigMapper labFlowConfigMapper;

    private final LabFlowNodeMapper labFlowNodeMapper;

    private final LabRoleMapper labRoleMapper;

    private final LabUserMapper labUserMapper;

    /**
     * 分页查询流程配置。
     *
     * @param query 查询条件
     * @return 流程配置分页结果
     */
    public PageResult<FlowConfigVO> page(FlowConfigQuery query) {
        String keyword = StrUtil.trim(query.getKeyword());
        Set<Long> matchedFlowIds = findFlowIdsByNodeKeyword(keyword);
        Page<LabFlowConfig> page = labFlowConfigMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabFlowConfig>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> {
                            wrapper.like(LabFlowConfig::getFlowName, keyword)
                                    .or()
                                    .like(LabFlowConfig::getScopeName, keyword)
                                    .or()
                                    .like(LabFlowConfig::getRemark, keyword);
                            if (!matchedFlowIds.isEmpty()) {
                                wrapper.or().in(LabFlowConfig::getId, matchedFlowIds);
                            }
                        })
                        .eq(StrUtil.isNotBlank(query.getFlowType()), LabFlowConfig::getFlowType, StrUtil.trim(query.getFlowType()))
                        .eq(query.getStatus() != null, LabFlowConfig::getStatus, query.getStatus())
                        .orderByDesc(LabFlowConfig::getDefaultFlag)
                        .orderByAsc(LabFlowConfig::getFlowType)
                        .orderByDesc(LabFlowConfig::getUpdatedTime)
                        .orderByDesc(LabFlowConfig::getCreatedTime));
        Map<Long, List<LabFlowNode>> nodeMap = loadNodeMap(page.getRecords().stream()
                .map(LabFlowConfig::getId)
                .collect(Collectors.toList()));
        List<FlowConfigVO> records = page.getRecords().stream()
                .map(item -> toVO(item, nodeMap.get(item.getId())))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取流程详情。
     *
     * @param id 流程ID
     * @return 流程详情
     */
    public FlowConfigVO detail(Long id) {
        LabFlowConfig entity = requireFlow(id);
        return toVO(entity, loadNodes(id));
    }

    /**
     * 查询启用流程下拉选项。
     *
     * @param flowType 流程类型
     * @return 流程选项
     */
    public List<FlowConfigOptionVO> options(String flowType) {
        return labFlowConfigMapper.selectList(new LambdaQueryWrapper<LabFlowConfig>()
                        .eq(LabFlowConfig::getStatus, 1)
                        .eq(StrUtil.isNotBlank(flowType), LabFlowConfig::getFlowType, StrUtil.trim(flowType))
                        .orderByDesc(LabFlowConfig::getDefaultFlag)
                        .orderByAsc(LabFlowConfig::getFlowName))
                .stream()
                .map(this::toOption)
                .collect(Collectors.toList());
    }

    /**
     * 新增流程配置。
     *
     * @param command 保存命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(FlowConfigSaveCommand command) {
        validateFlowType(command.getFlowType());
        validateFlowNameUnique(StrUtil.trim(command.getFlowName()), null);

        LabFlowConfig entity = new LabFlowConfig();
        applyCommand(entity, command);
        if (Boolean.TRUE.equals(command.getDefaultFlag())) {
            clearDefault(entity.getFlowType(), null);
        }
        labFlowConfigMapper.insert(entity);
        replaceNodes(entity.getId(), command.getNodes());
    }

    /**
     * 更新流程配置。
     *
     * @param id 流程ID
     * @param command 保存命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, FlowConfigSaveCommand command) {
        validateFlowType(command.getFlowType());
        LabFlowConfig entity = requireFlow(id);
        validateFlowNameUnique(StrUtil.trim(command.getFlowName()), id);
        applyCommand(entity, command);
        if (Boolean.TRUE.equals(command.getDefaultFlag())) {
            clearDefault(entity.getFlowType(), id);
        }
        labFlowConfigMapper.updateById(entity);
        replaceNodes(id, command.getNodes());
    }

    /**
     * 保存流程节点。
     *
     * @param id 流程ID
     * @param nodes 节点列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveNodes(Long id, List<FlowNodeCommand> nodes) {
        requireFlow(id);
        replaceNodes(id, nodes);
    }

    /**
     * 切换启停状态。
     *
     * @param id 流程ID
     * @param status 状态
     */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("流程状态不正确");
        }
        LabFlowConfig entity = requireFlow(id);
        entity.setStatus(status);
        labFlowConfigMapper.updateById(entity);
    }

    /**
     * 设置默认流程。
     *
     * @param id 流程ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id) {
        LabFlowConfig entity = requireFlow(id);
        clearDefault(entity.getFlowType(), id);
        entity.setDefaultFlag(1);
        entity.setStatus(1);
        labFlowConfigMapper.updateById(entity);
    }

    /**
     * 删除流程配置。
     *
     * @param id 流程ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        LabFlowConfig entity = requireFlow(id);
        labFlowNodeMapper.delete(new LambdaQueryWrapper<LabFlowNode>().eq(LabFlowNode::getFlowId, entity.getId()));
        labFlowConfigMapper.deleteById(entity.getId());
    }

    private Set<Long> findFlowIdsByNodeKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptySet();
        }
        return labFlowNodeMapper.selectList(new LambdaQueryWrapper<LabFlowNode>()
                        .like(LabFlowNode::getNodeName, keyword)
                        .or()
                        .like(LabFlowNode::getRoleName, keyword)
                        .or()
                        .like(LabFlowNode::getAssigneeName, keyword))
                .stream()
                .map(LabFlowNode::getFlowId)
                .collect(Collectors.toSet());
    }

    private Map<Long, List<LabFlowNode>> loadNodeMap(List<Long> flowIds) {
        if (flowIds == null || flowIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labFlowNodeMapper.selectList(new LambdaQueryWrapper<LabFlowNode>()
                        .in(LabFlowNode::getFlowId, flowIds)
                        .orderByAsc(LabFlowNode::getNodeOrder)
                        .orderByAsc(LabFlowNode::getCreatedTime))
                .stream()
                .collect(Collectors.groupingBy(LabFlowNode::getFlowId));
    }

    private List<LabFlowNode> loadNodes(Long flowId) {
        return labFlowNodeMapper.selectList(new LambdaQueryWrapper<LabFlowNode>()
                .eq(LabFlowNode::getFlowId, flowId)
                .orderByAsc(LabFlowNode::getNodeOrder)
                .orderByAsc(LabFlowNode::getCreatedTime));
    }

    private void replaceNodes(Long flowId, List<FlowNodeCommand> nodes) {
        labFlowNodeMapper.delete(new LambdaQueryWrapper<LabFlowNode>().eq(LabFlowNode::getFlowId, flowId));
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        int order = 1;
        for (FlowNodeCommand nodeCommand : nodes) {
            validateNodeCommand(nodeCommand);
            LabFlowNode node = new LabFlowNode();
            node.setFlowId(flowId);
            node.setNodeOrder(order++);
            node.setNodeName(StrUtil.trim(nodeCommand.getNodeName()));
            node.setRoleName(StrUtil.trim(nodeCommand.getRoleName()));
            node.setRoleCode(StrUtil.trim(nodeCommand.getRoleCode()));
            node.setAssigneeId(nodeCommand.getAssigneeId());
            node.setAssigneeName(StrUtil.trim(nodeCommand.getAssigneeName()));
            node.setRequiredFlag(Boolean.FALSE.equals(nodeCommand.getRequired()) ? 0 : 1);
            node.setRejectMode(StrUtil.trim(nodeCommand.getRejectMode()));
            labFlowNodeMapper.insert(node);
        }
    }

    private void validateNodeCommand(FlowNodeCommand nodeCommand) {
        if (nodeCommand == null) {
            throw new BusinessException("流程节点不能为空");
        }
        if (StrUtil.isBlank(nodeCommand.getNodeName())) {
            throw new BusinessException("节点名称不能为空");
        }
        if (StrUtil.isBlank(nodeCommand.getRoleName())) {
            throw new BusinessException("审批角色不能为空");
        }
        if (StrUtil.isBlank(nodeCommand.getRoleCode())) {
            throw new BusinessException("审批角色编码不能为空");
        }
        LabRole role = labRoleMapper.selectOne(new LambdaQueryWrapper<LabRole>()
                .eq(LabRole::getRoleCode, StrUtil.trim(nodeCommand.getRoleCode()))
                .last("limit 1"));
        if (role == null) {
            throw new BusinessException("审批角色不存在，请刷新后重新选择");
        }
        if (role.getStatus() == null || role.getStatus() != 1) {
            throw new BusinessException("审批角色已停用，请重新选择");
        }
        if (!StrUtil.equals(StrUtil.trim(nodeCommand.getRoleName()), role.getRoleName())) {
            throw new BusinessException("审批角色名称与系统配置不一致，请刷新后重新选择");
        }
        if (nodeCommand.getAssigneeId() != null) {
            LabUser user = labUserMapper.selectById(nodeCommand.getAssigneeId());
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                throw new BusinessException("指定人员不存在或已停用，请重新选择");
            }
            if (!StrUtil.equals(StrUtil.trim(nodeCommand.getRoleCode()), user.getRoleCode())) {
                throw new BusinessException("指定人员不属于所选审批角色，请重新选择");
            }
            if (StrUtil.isNotBlank(nodeCommand.getAssigneeName())
                    && !StrUtil.equals(StrUtil.trim(nodeCommand.getAssigneeName()), user.getRealName())) {
                throw new BusinessException("指定人员名称与系统配置不一致，请刷新后重新选择");
            }
        }
        if (nodeCommand.getRequired() == null) {
            throw new BusinessException("是否必审不能为空");
        }
        if (StrUtil.isBlank(nodeCommand.getRejectMode())) {
            throw new BusinessException("驳回方式不能为空");
        }
    }

    private LabFlowConfig requireFlow(Long id) {
        LabFlowConfig entity = labFlowConfigMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("流程配置不存在");
        }
        return entity;
    }

    private void validateFlowType(String flowType) {
        String value = StrUtil.trim(flowType);
        if (!FLOW_TYPE_REVIEW.equals(value) && !FLOW_TYPE_PUBLISH.equals(value)) {
            throw new BusinessException("流程类型不正确");
        }
    }

    private void validateFlowNameUnique(String flowName, Long excludeId) {
        Long count = labFlowConfigMapper.selectCount(new LambdaQueryWrapper<LabFlowConfig>()
                .eq(LabFlowConfig::getFlowName, flowName)
                .ne(excludeId != null, LabFlowConfig::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("流程名称已存在");
        }
    }

    private void clearDefault(String flowType, Long excludeId) {
        List<LabFlowConfig> defaults = labFlowConfigMapper.selectList(new LambdaQueryWrapper<LabFlowConfig>()
                .eq(LabFlowConfig::getFlowType, flowType)
                .eq(LabFlowConfig::getDefaultFlag, 1)
                .ne(excludeId != null, LabFlowConfig::getId, excludeId));
        defaults.forEach(item -> {
            item.setDefaultFlag(0);
            labFlowConfigMapper.updateById(item);
        });
    }

    private void applyCommand(LabFlowConfig entity, FlowConfigSaveCommand command) {
        entity.setFlowName(StrUtil.trim(command.getFlowName()));
        entity.setFlowType(StrUtil.trim(command.getFlowType()));
        entity.setScopeName(StrUtil.trim(command.getScopeName()));
        entity.setDefaultFlag(Boolean.TRUE.equals(command.getDefaultFlag()) ? 1 : 0);
        entity.setStatus(command.getStatus() == null || command.getStatus() != 0 ? 1 : 0);
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private FlowConfigVO toVO(LabFlowConfig entity, List<LabFlowNode> nodes) {
        FlowConfigVO vo = new FlowConfigVO();
        vo.setId(entity.getId());
        vo.setFlowName(entity.getFlowName());
        vo.setFlowType(entity.getFlowType());
        vo.setScopeName(entity.getScopeName());
        vo.setDefaultFlag(entity.getDefaultFlag() != null && entity.getDefaultFlag() == 1);
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setCreatedTime(entity.getCreatedTime());
        vo.setUpdatedTime(entity.getUpdatedTime());
        List<FlowNodeVO> nodeVOList = (nodes == null ? Collections.<LabFlowNode>emptyList() : nodes)
                .stream()
                .map(this::toNodeVO)
                .collect(Collectors.toList());
        vo.setNodes(nodeVOList);
        vo.setNodeCount(nodeVOList.size());
        return vo;
    }

    private FlowNodeVO toNodeVO(LabFlowNode entity) {
        FlowNodeVO vo = new FlowNodeVO();
        vo.setId(entity.getId());
        vo.setNodeOrder(entity.getNodeOrder());
        vo.setNodeName(entity.getNodeName());
        vo.setRoleName(entity.getRoleName());
        vo.setRoleCode(entity.getRoleCode());
        vo.setAssigneeId(entity.getAssigneeId());
        vo.setAssigneeName(entity.getAssigneeName());
        vo.setRequired(entity.getRequiredFlag() == null || entity.getRequiredFlag() == 1);
        vo.setRejectMode(entity.getRejectMode());
        return vo;
    }

    private FlowConfigOptionVO toOption(LabFlowConfig entity) {
        FlowConfigOptionVO vo = new FlowConfigOptionVO();
        vo.setId(entity.getId());
        vo.setFlowName(entity.getFlowName());
        vo.setFlowType(entity.getFlowType());
        vo.setDefaultFlag(entity.getDefaultFlag() != null && entity.getDefaultFlag() == 1);
        return vo;
    }
}
