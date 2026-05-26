package com.yx.lab.modules.mobile.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.mobile.vo.MobileReportVO;
import com.yx.lab.modules.report.dto.ReportQuery;
import com.yx.lab.modules.report.entity.LabReport;
import com.yx.lab.modules.report.mapper.LabReportMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 移动端报告查询服务。
 */
@Service
@RequiredArgsConstructor
public class MobileReportQueryService {

    private final LabReportMapper labReportMapper;

    private final LabSampleMapper labSampleMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    /**
     * 分页查询当前用户可见的移动端报告。
     *
     * @param query 报告分页查询条件
     * @return 报告分页结果
     */
    public PageResult<MobileReportVO> reportMine(ReportQuery query) {
        CurrentUser currentUser = requireCurrentUser();
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());

        LambdaQueryWrapper<LabReport> wrapper = new LambdaQueryWrapper<LabReport>()
                .and(StrUtil.isNotBlank(keyword), condition -> condition
                        .like(LabReport::getReportName, keyword)
                        .or()
                        .like(LabReport::getSealNo, keyword)
                        .or()
                        .like(LabReport::getSampleNo, keyword))
                .eq(StrUtil.isNotBlank(query.getReportType()), LabReport::getReportType, query.getReportType())
                .eq(StrUtil.isNotBlank(query.getReportStatus()), LabReport::getReportStatus, query.getReportStatus())
                .orderByDesc(LabReport::getGeneratedTime);
        if (!isAdmin(currentUser)) {
            Set<Long> sampleIds = new LinkedHashSet<>();
            sampleIds.addAll(labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                            .eq(LabSample::getSamplerId, currentUser.getUserId()))
                    .stream()
                    .map(LabSample::getId)
                    .collect(Collectors.toList()));
            sampleIds.addAll(reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                            .eq(ReviewRecord::getReviewerId, currentUser.getUserId()))
                    .stream()
                    .map(ReviewRecord::getSampleId)
                    .filter(sampleId -> sampleId != null)
                    .collect(Collectors.toList()));
            if (!sampleIds.isEmpty()) {
                wrapper.and(condition -> condition
                        .in(LabReport::getSampleId, new ArrayList<>(sampleIds))
                        .or()
                        .eq(LabReport::getPublishedBy, currentUser.getUserId()));
            } else {
                wrapper.eq(LabReport::getPublishedBy, currentUser.getUserId());
            }
        }

        Page<LabReport> page = labReportMapper.selectPage(PageUtils.buildPage(query), wrapper);
        return new PageResult<>(page.getTotal(), page.getRecords().stream()
                .map(this::toMobileReportVO)
                .collect(Collectors.toList()));
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        return currentUser;
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleCode());
    }

    private MobileReportVO toMobileReportVO(LabReport report) {
        MobileReportVO vo = new MobileReportVO();
        vo.setId(report.getId());
        vo.setSampleId(report.getSampleId());
        vo.setReportName(report.getReportName());
        vo.setSampleNo(report.getSampleNo());
        vo.setSealNo(report.getSealNo());
        vo.setReportStatus(report.getReportStatus());
        vo.setReportStatusDesc(LabWorkflowConstants.getReportStatusLabel(report.getReportStatus()));
        vo.setGeneratedTime(report.getGeneratedTime());
        vo.setPublishedTime(report.getPublishedTime());
        vo.setPublishedByName(report.getPublishedByName());
        return vo;
    }
}