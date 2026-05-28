package com.yx.lab.modules.dashboard.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 检测员首页数据，面向检测员个人工作台汇总待办、完成量和最近记录。
 */
@Data
@Schema(description = "检测员首页数据")
public class DetectorDashboardVO {

    @Schema(description = "当前角色编码")
    private String roleCode;

    @Schema(description = "个人统计")
    private StatsVO stats;

    @Schema(description = "待检测任务")
    private List<TodoItemVO> todoItems;

    @Schema(description = "最近检测记录")
    private List<RecentRecordVO> recentRecords;

    /**
     * 检测员个人统计。
     */
    @Data
    @Schema(description = "检测员个人统计")
    public static class StatsVO {

        @Schema(description = "待检测数量")
        private Long pendingCount;

        @Schema(description = "今日完成数量")
        private Long todayFinishCount;

        @Schema(description = "本周完成数量")
        private Long weekFinishCount;

        @Schema(description = "本月完成数量")
        private Long monthFinishCount;

        @Schema(description = "平均耗时，单位分钟")
        private Long averageMinutes;
    }

    /**
     * 检测员待办项。
     */
    @Data
    @Schema(description = "检测员待办项")
    public static class TodoItemVO {

        @Schema(description = "检测子流程ID")
        private Long id;

        @Schema(description = "主流程ID")
        private Long recordId;

        @Schema(description = "标题")
        private String title;

        @Schema(description = "样品编号")
        private String sampleNo;

        @Schema(description = "检测参数")
        private String parameterName;

        @Schema(description = "检测方法")
        private String methodName;

        @Schema(description = "辅助展示信息")
        private String meta;

        @Schema(description = "状态编码")
        private String status;

        @Schema(description = "状态名称")
        private String statusText;
    }

    /**
     * 检测员最近检测记录。
     */
    @Data
    @Schema(description = "检测员最近检测记录")
    public static class RecentRecordVO {

        @Schema(description = "检测子流程ID")
        private Long id;

        @Schema(description = "主流程ID")
        private Long recordId;

        @Schema(description = "样品编号")
        private String sampleNo;

        @Schema(description = "检测参数展示文本")
        private String parameterText;

        @Schema(description = "检测结果")
        private String resultValue;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "判定结果")
        private String resultText;

        @Schema(description = "状态编码")
        private String status;

        @Schema(description = "状态名称")
        private String statusText;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "更新时间")
        private LocalDateTime updatedTime;
    }
}
