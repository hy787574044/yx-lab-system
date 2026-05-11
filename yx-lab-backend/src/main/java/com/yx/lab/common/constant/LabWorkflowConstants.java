package com.yx.lab.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class LabWorkflowConstants {

    private LabWorkflowConstants() {
    }

    public static final class InstrumentStatus {

        public static final String NORMAL = "NORMAL";

        public static final String DISABLED = "DISABLED";

        public static final String MAINTENANCE = "MAINTENANCE";

        public static final String CALIBRATING = "CALIBRATING";

        private InstrumentStatus() {
        }
    }

    public static final class PointStatus {

        public static final String ENABLED = "ENABLED";

        public static final String DISABLED = "DISABLED";

        private PointStatus() {
        }
    }

    public static final class PointType {

        public static final String FACTORY = "FACTORY";

        public static final String RAW = "RAW";

        public static final String TERMINAL = "TERMINAL";

        private PointType() {
        }
    }

    public static final class FrequencyType {

        public static final String DAILY = "DAILY";

        public static final String WEEKLY = "WEEKLY";

        public static final String MONTHLY = "MONTHLY";

        private FrequencyType() {
        }
    }

    public static final class SamplingPlanStatus {

        public static final String ACTIVE = "ACTIVE";

        public static final String PAUSED = "PAUSED";

        public static final String DISPATCHED = "DISPATCHED";

        public static final String COMPLETED = "COMPLETED";

        public static final String UNPUBLISHED = "UNPUBLISHED";

        private SamplingPlanStatus() {
        }
    }

    public static final class SamplingTaskStatus {

        public static final String PENDING = "PENDING";

        public static final String IN_PROGRESS = "IN_PROGRESS";

        public static final String ABANDONED = "ABANDONED";

        public static final String COMPLETED = "COMPLETED";

        private SamplingTaskStatus() {
        }
    }

    public static final class SampleRegisterStatus {

        public static final String UNREGISTERED = "UNREGISTERED";

        public static final String REGISTERED = "REGISTERED";

        private SampleRegisterStatus() {
        }
    }

    public static final class SampleType {

        public static final String FACTORY = "FACTORY";

        public static final String RAW = "RAW";

        public static final String TERMINAL = "TERMINAL";

        private SampleType() {
        }
    }

    public static final class SamplingType {

        public static final String ROUTINE = "ROUTINE";

        private SamplingType() {
        }
    }

    public static final class SampleStatus {

        public static final String LOGGED = "LOGGED";

        public static final String REVIEWING = "REVIEWING";

        public static final String RETEST = "RETEST";

        public static final String COMPLETED = "COMPLETED";

        private SampleStatus() {
        }
    }

    public static final class DetectionStatus {

        public static final String WAIT_ASSIGN = "WAIT_ASSIGN";

        public static final String WAIT_DETECT = "WAIT_DETECT";

        public static final String SUBMITTED = "SUBMITTED";

        public static final String APPROVED = "APPROVED";

        public static final String REJECTED = "REJECTED";

        private DetectionStatus() {
        }
    }

    public static final class DetectionResult {

        public static final String NORMAL = "NORMAL";

        public static final String ABNORMAL = "ABNORMAL";

        private DetectionResult() {
        }
    }

    public static final class ReviewResult {

        public static final String APPROVED = "APPROVED";

        public static final String REJECTED = "REJECTED";

        private ReviewResult() {
        }
    }

    public static final class ReportStatus {

        public static final String DRAFT = "DRAFT";

        public static final String GENERATED = "GENERATED";

        public static final String PUBLISHED = "PUBLISHED";

        private ReportStatus() {
        }
    }

    public static final class ReportType {

        public static final String DAILY = "DAILY";

        public static final String WEEKLY = "WEEKLY";

        public static final String MONTHLY = "MONTHLY";

        private ReportType() {
        }
    }

    public static final class CycleType {

        public static final String ONCE = "ONCE";

        public static final String DAILY = "DAILY";

        public static final String WEEKLY = "WEEKLY";

        public static final String MONTHLY = "MONTHLY";

        private CycleType() {
        }
    }

    public static final Set<String> INSTRUMENT_STATUSES = unmodifiableSet(
            InstrumentStatus.NORMAL,
            InstrumentStatus.DISABLED,
            InstrumentStatus.MAINTENANCE,
            InstrumentStatus.CALIBRATING);

    public static final Set<String> POINT_STATUSES = unmodifiableSet(
            PointStatus.ENABLED,
            PointStatus.DISABLED);

    public static final Set<String> POINT_TYPES = unmodifiableSet(
            PointType.FACTORY,
            PointType.RAW,
            PointType.TERMINAL);

    public static final Set<String> FREQUENCY_TYPES = unmodifiableSet(
            FrequencyType.DAILY,
            FrequencyType.WEEKLY,
            FrequencyType.MONTHLY);

    public static final Set<String> CYCLE_TYPES = unmodifiableSet(
            CycleType.ONCE,
            CycleType.DAILY,
            CycleType.WEEKLY,
            CycleType.MONTHLY);

    public static final Set<String> SAMPLE_TYPES = unmodifiableSet(
            SampleType.FACTORY,
            SampleType.RAW,
            SampleType.TERMINAL);

    public static final Set<String> REPORT_TYPES = unmodifiableSet(
            ReportType.DAILY,
            ReportType.WEEKLY,
            ReportType.MONTHLY);

    public static final Set<String> PAUSABLE_PLAN_STATUSES = unmodifiableSet(
            SamplingPlanStatus.ACTIVE,
            SamplingPlanStatus.UNPUBLISHED);

    public static final Set<String> LOCKED_PLAN_STATUSES = unmodifiableSet(
            SamplingPlanStatus.DISPATCHED,
            SamplingPlanStatus.COMPLETED);

    public static final Set<String> DISPATCHABLE_PLAN_STATUSES = unmodifiableSet(
            SamplingPlanStatus.ACTIVE,
            SamplingPlanStatus.UNPUBLISHED);

    public static final Set<String> TODO_TASK_STATUSES = unmodifiableSet(
            SamplingTaskStatus.PENDING,
            SamplingTaskStatus.IN_PROGRESS);

    public static final Set<String> COMPLETABLE_TASK_STATUSES = unmodifiableSet(
            SamplingTaskStatus.PENDING,
            SamplingTaskStatus.IN_PROGRESS);

    public static final Set<String> DETECTABLE_SAMPLE_STATUSES = unmodifiableSet(
            SampleStatus.LOGGED,
            SampleStatus.RETEST);

    public static final Set<String> ACTIVE_DETECTION_RECORD_STATUSES = unmodifiableSet(
            DetectionStatus.WAIT_ASSIGN,
            DetectionStatus.WAIT_DETECT,
            DetectionStatus.SUBMITTED);

    public static boolean isKnownInstrumentStatus(String status) {
        return INSTRUMENT_STATUSES.contains(status);
    }

    public static boolean canPausePlan(String planStatus) {
        return PAUSABLE_PLAN_STATUSES.contains(planStatus);
    }

    public static boolean canResumePlan(String planStatus) {
        return SamplingPlanStatus.PAUSED.equals(planStatus);
    }

    public static boolean canDispatchPlan(String planStatus) {
        return DISPATCHABLE_PLAN_STATUSES.contains(planStatus);
    }

    public static boolean isLockedPlan(String planStatus) {
        return LOCKED_PLAN_STATUSES.contains(planStatus);
    }

    public static boolean canStartTask(String taskStatus) {
        return SamplingTaskStatus.PENDING.equals(taskStatus);
    }

    public static boolean canAbandonTask(String taskStatus) {
        return COMPLETABLE_TASK_STATUSES.contains(taskStatus);
    }

    public static boolean canResumeTask(String taskStatus) {
        return SamplingTaskStatus.ABANDONED.equals(taskStatus);
    }

    public static boolean canCompleteTask(String taskStatus) {
        return SamplingTaskStatus.IN_PROGRESS.equals(taskStatus);
    }

    public static boolean canSubmitDetection(String sampleStatus) {
        return DETECTABLE_SAMPLE_STATUSES.contains(sampleStatus);
    }

    public static boolean canReviewDetection(String detectionStatus) {
        return DetectionStatus.SUBMITTED.equals(detectionStatus);
    }

    public static boolean canAssignDetection(String detectionStatus) {
        return DetectionStatus.WAIT_ASSIGN.equals(detectionStatus)
                || DetectionStatus.WAIT_DETECT.equals(detectionStatus);
    }

    public static String detectionStatusForReviewResult(String reviewResult) {
        return ReviewResult.APPROVED.equals(reviewResult)
                ? DetectionStatus.APPROVED
                : DetectionStatus.REJECTED;
    }

    public static String sampleStatusForReviewResult(String reviewResult) {
        return ReviewResult.APPROVED.equals(reviewResult)
                ? SampleStatus.COMPLETED
                : SampleStatus.RETEST;
    }

    public static boolean canPublishReport(String reportStatus) {
        return ReportStatus.DRAFT.equals(reportStatus)
                || ReportStatus.GENERATED.equals(reportStatus);
    }

    public static boolean canUnpublishReport(String reportStatus) {
        return ReportStatus.PUBLISHED.equals(reportStatus);
    }

    public static boolean isRecurringCycle(String cycleType) {
        return CycleType.DAILY.equals(cycleType)
                || CycleType.WEEKLY.equals(cycleType)
                || CycleType.MONTHLY.equals(cycleType);
    }

    public static boolean isOnceCycle(String cycleType) {
        return CycleType.ONCE.equals(cycleType);
    }

    public static String getSampleTypeLabel(String sampleType) {
        if (SampleType.FACTORY.equals(sampleType)) {
            return "出厂水";
        }
        if (SampleType.RAW.equals(sampleType)) {
            return "原水";
        }
        if (SampleType.TERMINAL.equals(sampleType)) {
            return "管网末梢";
        }
        return sampleType;
    }

    public static String getSampleStatusLabel(String sampleStatus) {
        if (SampleStatus.LOGGED.equals(sampleStatus)) {
            return "已登录";
        }
        if (SampleStatus.REVIEWING.equals(sampleStatus)) {
            return "审核中";
        }
        if (SampleStatus.RETEST.equals(sampleStatus)) {
            return "待重检";
        }
        if (SampleStatus.COMPLETED.equals(sampleStatus)) {
            return "已完成";
        }
        return sampleStatus;
    }

    public static String getDetectionStatusLabel(String detectionStatus) {
        if (DetectionStatus.WAIT_ASSIGN.equals(detectionStatus)) {
            return "待分配";
        }
        if (DetectionStatus.WAIT_DETECT.equals(detectionStatus)) {
            return "待检测";
        }
        if (DetectionStatus.SUBMITTED.equals(detectionStatus)) {
            return "待审核";
        }
        if (DetectionStatus.APPROVED.equals(detectionStatus)) {
            return "审核通过";
        }
        if (DetectionStatus.REJECTED.equals(detectionStatus)) {
            return "审核驳回";
        }
        return detectionStatus;
    }

    public static String getDetectionResultLabel(String detectionResult) {
        if (DetectionResult.NORMAL.equals(detectionResult)) {
            return "正常";
        }
        if (DetectionResult.ABNORMAL.equals(detectionResult)) {
            return "异常";
        }
        return detectionResult;
    }

    public static String getReviewResultLabel(String reviewResult) {
        if (ReviewResult.APPROVED.equals(reviewResult)) {
            return "审核通过";
        }
        if (ReviewResult.REJECTED.equals(reviewResult)) {
            return "审核驳回";
        }
        return reviewResult;
    }

    public static String getReportStatusLabel(String reportStatus) {
        if (ReportStatus.DRAFT.equals(reportStatus)) {
            return "草稿";
        }
        if (ReportStatus.GENERATED.equals(reportStatus)) {
            return "已生成";
        }
        if (ReportStatus.PUBLISHED.equals(reportStatus)) {
            return "已发布";
        }
        return reportStatus;
    }

    public static String getPushStatusLabel(String pushStatus) {
        if ("PENDING".equals(pushStatus)) {
            return "待推送";
        }
        if ("SUCCESS".equals(pushStatus)) {
            return "已推送";
        }
        if ("FAILED".equals(pushStatus)) {
            return "推送失败";
        }
        if ("CANCELLED".equals(pushStatus)) {
            return "已撤回";
        }
        return pushStatus;
    }

    private static Set<String> unmodifiableSet(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
