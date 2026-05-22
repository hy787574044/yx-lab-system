package com.yx.lab.modules.detection.controller;

import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.model.ApiResponse;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.ExcelExportUtil;
import com.yx.lab.modules.detection.dto.DetectionAssignCommand;
import com.yx.lab.modules.detection.dto.DetectionItemQuery;
import com.yx.lab.modules.detection.dto.DetectionRecordQuery;
import com.yx.lab.modules.detection.dto.DetectionSubmitCommand;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.service.DetectionWorkflowService;
import com.yx.lab.modules.detection.vo.DetectionItemPageVO;
import com.yx.lab.modules.detection.vo.DetectionItemSummaryVO;
import com.yx.lab.modules.detection.vo.DetectionRecordDetailVO;
import com.yx.lab.modules.detection.vo.DetectionRecordSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;

/**
 * 检测流程控制器。
 */
@RestController
@RequestMapping("/api/detections")
@RequiredArgsConstructor
@Tag(name = "检测流程管理")
public class DetectionWorkflowController {

    private final DetectionWorkflowService detectionWorkflowService;

    /**
     * 分页查询检测主流程。
     */
    @GetMapping
    @Operation(summary = "检测主流程分页")
    public ApiResponse<PageResult<DetectionRecord>> page(@Validated DetectionRecordQuery query) {
        return ApiResponse.success(detectionWorkflowService.page(query));
    }

    /**
     * 汇总查询检测主流程状态统计。
     */
    @GetMapping("/summary")
    @Operation(summary = "检测主流程统计")
    public ApiResponse<DetectionRecordSummaryVO> summary(@Validated DetectionRecordQuery query) {
        return ApiResponse.success(detectionWorkflowService.summary(query));
    }

    /**
     * 导出检测主流程。
     */
    @GetMapping("/export")
    @Operation(summary = "导出检测主流程")
    public ResponseEntity<byte[]> export(@Validated DetectionRecordQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "检测流程.xlsx",
                "检测流程",
                detectionWorkflowService.page(query).getRecords(),
                Arrays.asList(
                        ExcelExportUtil.column("样品编号", DetectionRecord::getSampleNo),
                        ExcelExportUtil.column("封签编号", DetectionRecord::getSealNo),
                        ExcelExportUtil.column("检测套餐", DetectionRecord::getDetectionTypeName),
                        ExcelExportUtil.column("检测人员", DetectionRecord::getDetectorName),
                        ExcelExportUtil.column("流程状态", item -> LabWorkflowConstants.getDetectionStatusLabel(item.getDetectionStatus())),
                        ExcelExportUtil.column("检测结果", item -> LabWorkflowConstants.getDetectionResultLabel(item.getDetectionResult())),
                        ExcelExportUtil.column("流程时间", DetectionRecord::getDetectionTime),
                        ExcelExportUtil.column("异常说明", DetectionRecord::getAbnormalRemark),
                        ExcelExportUtil.column("更新时间", DetectionRecord::getUpdatedTime)
                ));
    }

    /**
     * 分页查询检测子流程。
     */
    @GetMapping("/items")
    @Operation(summary = "检测子流程分页")
    public ApiResponse<PageResult<DetectionItemPageVO>> itemPage(@Validated DetectionItemQuery query) {
        return ApiResponse.success(detectionWorkflowService.itemPage(query));
    }

    /**
     * 汇总查询检测子流程状态统计。
     */
    @GetMapping("/items/summary")
    @Operation(summary = "检测子流程统计")
    public ApiResponse<DetectionItemSummaryVO> itemSummary(@Validated DetectionItemQuery query) {
        return ApiResponse.success(detectionWorkflowService.itemSummary(query));
    }

    /**
     * 导出检测子流程。
     */
    @GetMapping("/items/export")
    @Operation(summary = "导出检测子流程")
    public ResponseEntity<byte[]> exportItems(@Validated DetectionItemQuery query) {
        ExcelExportUtil.prepareExportQuery(query);
        return ExcelExportUtil.buildResponse(
                "检测分析.xlsx",
                "检测分析",
                detectionWorkflowService.itemPage(query).getRecords(),
                Arrays.asList(
                        ExcelExportUtil.column("样品编号", DetectionItemPageVO::getSampleNo),
                        ExcelExportUtil.column("封签编号", DetectionItemPageVO::getSealNo),
                        ExcelExportUtil.column("检测套餐", DetectionItemPageVO::getDetectionTypeName),
                        ExcelExportUtil.column("检测参数", DetectionItemPageVO::getParameterName),
                        ExcelExportUtil.column("检测方法", DetectionItemPageVO::getMethodName),
                        ExcelExportUtil.column("检测步骤", DetectionItemPageVO::getMethodBasis),
                        ExcelExportUtil.column("检测人员", DetectionItemPageVO::getDetectorName),
                        ExcelExportUtil.column("标准范围", item -> formatStandardRange(item.getStandardMin(), item.getStandardMax())),
                        ExcelExportUtil.column("单位", DetectionItemPageVO::getUnit),
                        ExcelExportUtil.column("检测标准", DetectionItemPageVO::getReferenceStandard),
                        ExcelExportUtil.column("检测值", DetectionItemPageVO::getResultValue),
                        ExcelExportUtil.column("判定结果", this::formatResultLabel),
                        ExcelExportUtil.column("子流程状态", item -> LabWorkflowConstants.getDetectionStatusLabel(item.getItemStatus())),
                        ExcelExportUtil.column("说明", DetectionItemPageVO::getAbnormalRemark),
                        ExcelExportUtil.column("更新时间", DetectionItemPageVO::getUpdatedTime)
                ));
    }

    /**
     * 查询检测主流程详情。
     */
    @GetMapping("/{id}")
    @Operation(summary = "检测主流程详情")
    public ApiResponse<DetectionRecordDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.success(detectionWorkflowService.detail(id));
    }

    /**
     * 为检测主流程下的参数子流程分配检测员。
     */
    @PostMapping("/{id}/assignDetectors")
    @Operation(summary = "分配检测员")
    public ApiResponse<Void> assignDetectors(@PathVariable Long id, @Valid @RequestBody DetectionAssignCommand command) {
        detectionWorkflowService.assignDetectors(id, command);
        return ApiResponse.successMessage("检测员分配成功");
    }

    /**
     * 提交检测结果。
     */
    @PostMapping("/submit")
    @Operation(summary = "提交检测结果")
    public ApiResponse<Void> submit(@Valid @RequestBody DetectionSubmitCommand command) {
        detectionWorkflowService.submit(command);
        return ApiResponse.successMessage("检测提交成功");
    }

    private String formatStandardRange(java.math.BigDecimal min, java.math.BigDecimal max) {
        if (min != null && max != null) {
            return min.stripTrailingZeros().toPlainString() + " - " + max.stripTrailingZeros().toPlainString();
        }
        if (min != null) {
            return ">= " + min.stripTrailingZeros().toPlainString();
        }
        if (max != null) {
            return "<= " + max.stripTrailingZeros().toPlainString();
        }
        return "-";
    }

    private String formatResultLabel(DetectionItemPageVO item) {
        if (item == null || item.getResultValue() == null) {
            return "待录入";
        }
        return Integer.valueOf(1).equals(item.getExceedFlag()) ? "异常" : "正常";
    }
}
