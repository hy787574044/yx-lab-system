package com.yx.lab.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class LabWorkflowConstants {

    private LabWorkflowConstants() {
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

        private ReportType() {
        }
    }

    public static final class CycleType {

        public static final String ONCE = "ONCE";

        private CycleType() {
        }
    }

    public static final Set<String> DISPATCHABLE_PLAN_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    SamplingPlanStatus.ACTIVE,
                    SamplingPlanStatus.UNPUBLISHED)));

    public static final Set<String> TODO_TASK_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    SamplingTaskStatus.PENDING,
                    SamplingTaskStatus.IN_PROGRESS)));

    public static final Set<String> DETECTABLE_SAMPLE_STATUSES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    SampleStatus.LOGGED,
                    SampleStatus.RETEST)));
}
