package com.yx.lab.modules.dashboard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 化验室主任首页数据，面向管理视角汇总关键指标、流程节点、趋势排行与预警。
 */
@Data
@Schema(description = "主任首页管理视角数据")
public class LeaderDashboardVO {

    @Schema(description = "顶部关键指标卡")
    private List<TopMetricVO> topMetrics;

    @Schema(description = "样品检测全流程节点")
    private List<ProcessNodeVO> processNodes;

    @Schema(description = "水质合格率趋势")
    private List<TrendItemVO> passRateTrend;

    @Schema(description = "检测员工作量排行")
    private List<RankingItemVO> detectorWorkloadRanking;

    @Schema(description = "待处理预警")
    private List<WarningItemVO> warnings;

    @Schema(description = "快捷处理入口")
    private List<QuickTodoVO> quickTodos;

    /**
     * 顶部关键指标卡。
     */
    @Data
    @Schema(description = "顶部关键指标卡")
    public static class TopMetricVO {

        @Schema(description = "指标名称")
        private String label;

        @Schema(description = "今日数量")
        private Long todayValue;

        @Schema(description = "本月数量")
        private Long monthValue;

        @Schema(description = "今年数量")
        private Long yearValue;

        @Schema(description = "实时数量")
        private Long realtimeValue;

        @Schema(description = "指标说明")
        private String description;

        @Schema(description = "前端色彩类型")
        private String tone;

        @Schema(description = "跳转路径")
        private String path;
    }

    /**
     * 流程节点数量。
     */
    @Data
    @Schema(description = "流程节点数量")
    public static class ProcessNodeVO {

        @Schema(description = "节点序号")
        private Integer index;

        @Schema(description = "节点名称")
        private String label;

        @Schema(description = "节点数量")
        private Long value;

        @Schema(description = "节点状态说明")
        private String statusText;

        @Schema(description = "是否需要预警")
        private Boolean warning;

        @Schema(description = "跳转路径")
        private String path;
    }

    /**
     * 趋势数据点。
     */
    @Data
    @Schema(description = "趋势数据点")
    public static class TrendItemVO {

        @Schema(description = "横轴标签")
        private String label;

        @Schema(description = "统计值")
        private BigDecimal value;

        @Schema(description = "合格数量")
        private Long normalCount;

        @Schema(description = "不合格数量")
        private Long abnormalCount;

        @Schema(description = "目标值")
        private BigDecimal target;
    }

    /**
     * 排行数据项。
     */
    @Data
    @Schema(description = "排行数据项")
    public static class RankingItemVO {

        @Schema(description = "名称")
        private String name;

        @Schema(description = "数量")
        private Long value;
    }

    /**
     * 待处理预警。
     */
    @Data
    @Schema(description = "待处理预警")
    public static class WarningItemVO {

        @Schema(description = "预警类型")
        private String type;

        @Schema(description = "预警标题")
        private String title;

        @Schema(description = "预警内容")
        private String content;

        @Schema(description = "优先级")
        private String priority;

        @Schema(description = "预警数量")
        private Long count;

        @Schema(description = "跳转路径")
        private String path;
    }

    @Data
    @Schema(description = "快捷处理入口")
    public static class QuickTodoVO {

        @Schema(description = "唯一标识")
        private String key;

        @Schema(description = "入口名称")
        private String label;

        @Schema(description = "待处理数量")
        private Long count;

        @Schema(description = "说明")
        private String description;

        @Schema(description = "前端色彩类型")
        private String tone;

        @Schema(description = "跳转路径")
        private String path;

        @Schema(description = "跳转查询参数")
        private Map<String, String> query;
    }
}
