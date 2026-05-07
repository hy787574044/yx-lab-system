package com.yx.lab.modules.mobile.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.modules.mobile.vo.MobileReportVO;
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

@Service
@RequiredArgsConstructor
public class MobileReportQueryService {

    private final LabReportMapper labReportMapper;

    private final LabSampleMapper labSampleMapper;

    private final ReviewRecordMapper reviewRecordMapper;

    public List<MobileReportVO> reportMine() {
        CurrentUser currentUser = requireCurrentUser();
        LambdaQueryWrapper<LabReport> wrapper = new LambdaQueryWrapper<LabReport>()
                .orderByDesc(LabReport::getGeneratedTime)
                .last("limit 50");
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
                wrapper.and(query -> query.in(LabReport::getSampleId, new ArrayList<>(sampleIds))
                        .or()
                        .eq(LabReport::getPublishedBy, currentUser.getUserId()));
            } else {
                wrapper.eq(LabReport::getPublishedBy, currentUser.getUserId());
            }
        }
        return labReportMapper.selectList(wrapper)
                .stream()
                .map(this::toMobileReportVO)
                .collect(Collectors.toList());
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录");
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
        vo.setGeneratedTime(report.getGeneratedTime());
        vo.setPublishedTime(report.getPublishedTime());
        vo.setPublishedByName(report.getPublishedByName());
        vo.setPushStatus(report.getPushStatus());
        vo.setLastPushTime(report.getLastPushTime());
        vo.setLastPushMessage(report.getLastPushMessage());
        return vo;
    }
}
