package com.yx.lab.modules.detection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.detection.dto.DetectionItemCommand;
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
import com.yx.lab.modules.detection.dto.DetectionSubmitCommand;
import com.yx.lab.modules.detection.entity.DetectionItem;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.entity.DetectionType;
import com.yx.lab.modules.detection.mapper.DetectionItemMapper;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.detection.mapper.DetectionTypeMapper;
import com.yx.lab.modules.detection.vo.DetectionRecordDetailVO;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.service.LabSampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DetectionWorkflowService {

    private final DetectionRecordMapper detectionRecordMapper;

    private final DetectionItemMapper detectionItemMapper;

    private final DetectionTypeMapper detectionTypeMapper;

    private final LabSampleMapper labSampleMapper;

    private final LabSampleService labSampleService;

    public PageResult<DetectionRecord> page(DetectionRecordQuery query) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        Page<DetectionRecord> page = detectionRecordMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<DetectionRecord>()
                        .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper
                                .like(DetectionRecord::getSampleNo, query.getKeyword())
                                .or()
                                .like(DetectionRecord::getDetectionTypeName, query.getKeyword()))
                        .eq(StrUtil.isNotBlank(query.getDetectionStatus()), DetectionRecord::getDetectionStatus, query.getDetectionStatus())
                        .eq(Boolean.TRUE.equals(query.getMine()), DetectionRecord::getDetectorId, currentUser.getUserId())
                        .orderByDesc(DetectionRecord::getDetectionTime));
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public DetectionRecordDetailVO detail(Long id) {
        DetectionRecord record = detectionRecordMapper.selectById(id);
        List<DetectionItem> items = detectionItemMapper.selectList(new LambdaQueryWrapper<DetectionItem>()
                .eq(DetectionItem::getRecordId, id)
                .orderByAsc(DetectionItem::getCreatedTime));
        DetectionRecordDetailVO vo = new DetectionRecordDetailVO();
        vo.setRecord(record);
        vo.setItems(items);
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void submit(DetectionSubmitCommand command) {
        LabSample sample = labSampleMapper.selectById(command.getSampleId());
        if (sample == null) {
            throw new BusinessException("样品不存在");
        }
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        DetectionType detectionType = detectionTypeMapper.selectById(command.getDetectionTypeId());

        DetectionRecord record = new DetectionRecord();
        record.setSampleId(sample.getId());
        record.setSampleNo(sample.getSampleNo());
        record.setDetectionTypeId(command.getDetectionTypeId());
        record.setDetectionTypeName(detectionType == null ? command.getDetectionTypeName() : detectionType.getTypeName());
        record.setDetectionTime(LocalDateTime.now());
        record.setDetectorId(currentUser.getUserId());
        record.setDetectorName(currentUser.getRealName());
        record.setAbnormalRemark(command.getAbnormalRemark());
        record.setDetectionStatus("SUBMITTED");
        record.setDetectionResult(buildResult(command.getItems()));
        detectionRecordMapper.insert(record);

        for (DetectionItemCommand itemCommand : command.getItems()) {
            DetectionItem item = new DetectionItem();
            item.setRecordId(record.getId());
            item.setParameterId(itemCommand.getParameterId());
            item.setParameterName(itemCommand.getParameterName());
            item.setStandardMin(itemCommand.getStandardMin());
            item.setStandardMax(itemCommand.getStandardMax());
            item.setResultValue(itemCommand.getResultValue());
            item.setUnit(itemCommand.getUnit());
            item.setExceedFlag(isExceeded(itemCommand) ? 1 : 0);
            detectionItemMapper.insert(item);
        }
        labSampleService.updateStatus(sample.getId(), "REVIEWING", record.getDetectionResult());
    }

    private String buildResult(List<DetectionItemCommand> items) {
        return items.stream().anyMatch(this::isExceeded) ? "ABNORMAL" : "NORMAL";
    }

    private boolean isExceeded(DetectionItemCommand itemCommand) {
        if (itemCommand.getResultValue() == null) {
            return false;
        }
        if (itemCommand.getStandardMin() != null && itemCommand.getResultValue().compareTo(itemCommand.getStandardMin()) < 0) {
            return true;
        }
        return itemCommand.getStandardMax() != null
                && itemCommand.getResultValue().compareTo(itemCommand.getStandardMax()) > 0;
    }
}
