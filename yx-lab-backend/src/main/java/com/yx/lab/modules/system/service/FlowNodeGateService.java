package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.modules.system.entity.LabFlowConfig;
import com.yx.lab.modules.system.entity.LabFlowNode;
import com.yx.lab.modules.system.mapper.LabFlowConfigMapper;
import com.yx.lab.modules.system.mapper.LabFlowNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 流程节点处理门禁。
 */
@Service
@RequiredArgsConstructor
public class FlowNodeGateService {

    private final LabFlowConfigMapper labFlowConfigMapper;

    private final LabFlowNodeMapper labFlowNodeMapper;

    /**
     * 校验当前用户是否允许处理指定流程节点。
     *
     * @param flowId 流程ID
     * @param expectedFlowType 预期流程类型
     * @param currentUser 当前用户
     * @param actionName 操作名称
     */
    public void assertCanHandle(Long flowId, String expectedFlowType, CurrentUser currentUser, String actionName) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录用户信息失效，请重新登录");
        }
        if (isAdmin(currentUser)) {
            return;
        }
        // 兼容历史样品：旧数据没有流程ID时沿用原有处理规则。
        if (flowId == null) {
            return;
        }

        LabFlowConfig flow = labFlowConfigMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException(actionName + "流程配置不存在，请联系管理员检查流程配置");
        }
        if (!StrUtil.equals(expectedFlowType, flow.getFlowType())) {
            throw new BusinessException(actionName + "流程类型不正确，请联系管理员检查流程配置");
        }
        if (flow.getStatus() == null || flow.getStatus() != 1) {
            throw new BusinessException(actionName + "流程已停用，请联系管理员启用后再处理");
        }

        List<LabFlowNode> nodes = labFlowNodeMapper.selectList(new LambdaQueryWrapper<LabFlowNode>()
                .eq(LabFlowNode::getFlowId, flowId)
                .orderByAsc(LabFlowNode::getNodeOrder)
                .orderByAsc(LabFlowNode::getCreatedTime));
        if (nodes.isEmpty()) {
            throw new BusinessException(actionName + "流程未配置节点，请先在流程配置中维护节点");
        }
        boolean allowed = nodes.stream().anyMatch(node -> canHandleNode(node, currentUser));
        if (!allowed) {
            throw new BusinessException("当前用户无权处理该" + actionName + "节点，请确认是否属于节点角色或被指定为处理人员");
        }
    }

    /**
     * 解析当前应该处理的必审节点，并校验当前用户只能处理该节点。
     *
     * @param flowId 流程ID
     * @param expectedFlowType 预期流程类型
     * @param currentUser 当前用户
     * @param actionName 操作名称
     * @param approvedNodeIds 已审核通过的节点ID集合
     * @return 当前应处理的必审节点，未配置流程或无必审节点时返回空
     */
    public LabFlowNode resolveCurrentRequiredNode(Long flowId,
                                                  String expectedFlowType,
                                                  CurrentUser currentUser,
                                                  String actionName,
                                                  Set<Long> approvedNodeIds) {
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录用户信息失效，请重新登录");
        }
        if (flowId == null) {
            return null;
        }

        List<LabFlowNode> requiredNodes = loadRequiredNodes(flowId, expectedFlowType, actionName);
        if (requiredNodes.isEmpty()) {
            return null;
        }

        Set<Long> approvedIds = approvedNodeIds == null ? Collections.emptySet() : approvedNodeIds;
        LabFlowNode currentNode = requiredNodes.stream()
                .filter(node -> node.getId() != null && !approvedIds.contains(node.getId()))
                .findFirst()
                .orElse(null);
        if (currentNode == null) {
            throw new BusinessException(actionName + "流程已全部完成，请勿重复处理");
        }
        if (!isAdmin(currentUser) && !canHandleNode(currentNode, currentUser)) {
            throw new BusinessException("当前应处理节点为【"
                    + StrUtil.blankToDefault(currentNode.getNodeName(), "未命名节点")
                    + "】，请由该节点配置的角色或指定人员处理");
        }
        return currentNode;
    }

    /**
     * 判断指定流程中是否仍有未通过的必审节点。
     *
     * @param flowId 流程ID
     * @param expectedFlowType 预期流程类型
     * @param approvedNodeIds 已审核通过的节点ID集合
     * @return true 表示仍有必审节点未完成
     */
    public boolean hasRemainingRequiredNode(Long flowId, String expectedFlowType, Set<Long> approvedNodeIds) {
        if (flowId == null) {
            return false;
        }
        List<LabFlowNode> requiredNodes = loadRequiredNodes(flowId, expectedFlowType, "审核");
        if (requiredNodes.isEmpty()) {
            return false;
        }
        Set<Long> approvedIds = approvedNodeIds == null ? Collections.emptySet() : approvedNodeIds;
        return requiredNodes.stream()
                .anyMatch(node -> node.getId() != null && !approvedIds.contains(node.getId()));
    }

    private List<LabFlowNode> loadRequiredNodes(Long flowId, String expectedFlowType, String actionName) {
        LabFlowConfig flow = labFlowConfigMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException(actionName + "流程配置不存在，请联系管理员检查流程配置");
        }
        if (!StrUtil.equals(expectedFlowType, flow.getFlowType())) {
            throw new BusinessException(actionName + "流程类型不正确，请联系管理员检查流程配置");
        }
        if (flow.getStatus() == null || flow.getStatus() != 1) {
            throw new BusinessException(actionName + "流程已停用，请联系管理员启用后再处理");
        }

        List<LabFlowNode> nodes = labFlowNodeMapper.selectList(new LambdaQueryWrapper<LabFlowNode>()
                .eq(LabFlowNode::getFlowId, flowId)
                .orderByAsc(LabFlowNode::getNodeOrder)
                .orderByAsc(LabFlowNode::getCreatedTime));
        if (nodes.isEmpty()) {
            throw new BusinessException(actionName + "流程未配置节点，请先在流程配置中维护节点");
        }
        return nodes.stream()
                .filter(node -> node.getRequiredFlag() == null || node.getRequiredFlag() == 1)
                .collect(Collectors.toList());
    }

    private boolean canHandleNode(LabFlowNode node, CurrentUser currentUser) {
        if (node == null || currentUser == null) {
            return false;
        }
        if (node.getAssigneeId() != null) {
            return node.getAssigneeId().equals(currentUser.getUserId());
        }
        return StrUtil.isNotBlank(node.getRoleCode())
                && StrUtil.equalsIgnoreCase(StrUtil.trim(node.getRoleCode()), StrUtil.trim(currentUser.getRoleCode()));
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());
    }
}
