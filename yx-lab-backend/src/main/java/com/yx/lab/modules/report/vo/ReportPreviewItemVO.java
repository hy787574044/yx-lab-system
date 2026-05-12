package com.yx.lab.modules.report.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 报告预览中的检测结果明细行。
 */
@Data
@Schema(description = "报告预览中的检测结果明细行")
public class ReportPreviewItemVO {

    @Schema(description = "检测参数名称")
    private String parameterName;

    @Schema(description = "检测方法名称")
    private String methodName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "标准范围")
    private String standardRange;

    @Schema(description = "参考范围")
    private String referenceStandard;

    @Schema(description = "化验值")
    private String resultValue;

    @Schema(description = "结果对比")
    private String compareText;

    @Schema(description = "单项判定")
    private String judgmentLabel;

    @Schema(description = "子流程状态")
    private String itemStatusLabel;
}
