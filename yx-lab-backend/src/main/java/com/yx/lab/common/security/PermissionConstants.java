package com.yx.lab.common.security;

/**
 * 系统接口权限码。
 */
public final class PermissionConstants {

    public static final String ALL = "*";

    public static final String DASHBOARD_VIEW = "dashboard:view";

    public static final String SAMPLE_VIEW = "sample:view";
    public static final String SAMPLE_WRITE = "sample:write";

    public static final String SAMPLING_PLAN_VIEW = "samplingPlan:view";
    public static final String SAMPLING_PLAN_WRITE = "samplingPlan:write";
    public static final String SAMPLING_TASK_VIEW = "samplingTask:view";
    public static final String SAMPLING_TASK_WRITE = "samplingTask:write";

    public static final String DETECTION_VIEW = "detection:view";
    public static final String DETECTION_ASSIGN = "detection:assign";
    public static final String DETECTION_SUBMIT = "detection:submit";
    public static final String DETECTION_CONFIG_VIEW = "detectionConfig:view";
    public static final String DETECTION_CONFIG_WRITE = "detectionConfig:write";

    public static final String REVIEW_VIEW = "review:view";
    public static final String REVIEW_AUDIT = "review:audit";

    public static final String REPORT_VIEW = "report:view";
    public static final String REPORT_WRITE = "report:write";
    public static final String REPORT_PUBLISH = "report:publish";

    public static final String ASSET_VIEW = "asset:view";
    public static final String ASSET_WRITE = "asset:write";

    public static final String STATISTICS_VIEW = "statistics:view";

    public static final String SYSTEM_VIEW = "system:view";
    public static final String SYSTEM_WRITE = "system:write";

    private PermissionConstants() {
    }
}
