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

import java.util.List;

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
