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
        return COMPLETABLE_TASK_STATUSES.contains(taskStatus);
    }

    public static boolean canSubmitDetection(String sampleStatus) {
        return DETECTABLE_SAMPLE_STATUSES.contains(sampleStatus);
    }

    public static boolean canReviewDetection(String detectionStatus) {
        return DetectionStatus.SUBMITTED.equals(detectionStatus);
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

    private static Set<String> unmodifiableSet(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
