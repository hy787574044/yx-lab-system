package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.constant.LabWorkflowConstants;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.security.CurrentUser;
import com.yx.lab.common.security.DataScopeHelper;
import com.yx.lab.common.security.SecurityContext;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.sample.dto.SamplingTaskActionCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskCompleteCommand;
import com.yx.lab.modules.sample.dto.SamplingTaskQuery;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.entity.SamplingPlan;
import com.yx.lab.modules.sample.entity.SamplingTask;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.sample.mapper.SamplingPlanMapper;
import com.yx.lab.modules.sample.mapper.SamplingTaskMapper;
import com.yx.lab.modules.sample.vo.StatusCountVO;
import com.yx.lab.modules.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SamplingTaskService {

    private static final String TASK_SCOPE_TODO = "todo";

    private final SamplingTaskMapper samplingTaskMapper;

    private final LabSampleMapper labSampleMapper;

    private final SamplingPlanMapper samplingPlanMapper;

    private final SamplingPlanService samplingPlanService;

    private final StorageService storageService;

    private final DataScopeHelper dataScopeHelper;

    private final SampleNoGeneratorService sampleNoGeneratorService;

    public PageResult<SamplingTask> page(SamplingTaskQuery query) {
        if (isTodoScope(query)) {
            return pageTodoTasks(query);
        }
        Long scopedSamplerId = resolveScopedSamplerId(query.getSamplerId());
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .and(StrUtil.isNotBlank(query.getKeyword()), item -> item
                        .like(SamplingTask::getTaskNo, query.getKeyword())
                        .or()
                        .like(SamplingTask::getPointName, query.getKeyword())
                        .or()
                        .like(SamplingTask::getSampleNo, query.getKeyword()));
        wrapper.eq(query.getOrgId() != null, SamplingTask::getOrgId, query.getOrgId());
        applyTaskStatusFilter(wrapper, query.getTaskStatus());
        applySampleRegisterStatusFilter(wrapper, query.getSampleRegisterStatus());
        applyTaskSamplerScope(wrapper, scopedSamplerId);
        wrapper.orderByDesc(SamplingTask::getCreatedTime);
        Page<SamplingTask> page = samplingTaskMapper.selectPage(PageUtils.buildPage(query), wrapper);
        prepareTasksForView(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    public List<StatusCountVO> statusStats(SamplingTaskQuery query) {
        if (isTodoScope(query)) {
            return todoStatusStats(query);
        }
        return Arrays.asList(
                statusCount("ALL", countTasksByStatus(null)),
                statusCount("UNSAMPLED", countUnregisteredTasks()),
                statusCount("SAMPLED", countRegisteredTasks()),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.PENDING,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.COMPLETED,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.COMPLETED)),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.ABANDONED,
                        countTasksByStatus(LabWorkflowConstants.SamplingTaskStatus.ABANDONED)),
                statusCount("UNLOGGED", countUnloggedCompletedTasks()));
    }

    public List<StatusCountVO> statusStats() {
        return statusStats(null);
    }

    private StatusCountVO statusCount(String status, Long count) {
        StatusCountVO vo = new StatusCountVO();
        vo.setStatus(status);
        vo.setCount(count == null ? 0L : count);
        return vo;
    }

    private Long countTasksByStatus(String taskStatus) {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .eq(StrUtil.isNotBlank(taskStatus), SamplingTask::getTaskStatus, taskStatus);
        applyTaskSamplerScope(wrapper, resolveScopedSamplerId(null));
        Long count = samplingTaskMapper.selectCount(wrapper);
        return count == null ? 0L : count.longValue();
    }

    private Long countUnloggedCompletedTasks() {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .ne(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.ABANDONED);
        applyTaskSamplerScope(wrapper, resolveScopedSamplerId(null));
        List<SamplingTask> tasks = samplingTaskMapper.selectList(wrapper);
        return tasks.stream()
                .filter(task -> !isTaskRegistered(task))
                .count();
    }

    private Long countUnregisteredTasks() {
        return countUnloggedCompletedTasks();
    }

    private Long countRegisteredTasks() {
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .ne(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.ABANDONED);
        applyTaskSamplerScope(wrapper, resolveScopedSamplerId(null));
        List<SamplingTask> tasks = samplingTaskMapper.selectList(wrapper);
        return tasks.stream()
                .filter(this::isTaskRegistered)
                .count();
    }

    private Long resolveScopedSamplerId(Long querySamplerId) {
        if (dataScopeHelper.isAdmin()) {
            return querySamplerId;
        }
        if (dataScopeHelper.isRole("STAFF") && dataScopeHelper.currentUserId() != null) {
            return dataScopeHelper.currentUserId();
        }
        return querySamplerId;
    }

    private boolean isTodoScope(SamplingTaskQuery query) {
        return query != null && TASK_SCOPE_TODO.equalsIgnoreCase(StrUtil.trim(query.getScope()));
    }

    private PageResult<SamplingTask> pageTodoTasks(SamplingTaskQuery query) {
        List<SamplingTask> records = loadTodoTasks(query, false);
        long total = records.size();
        long pageNum = query == null || query.getPageNum() <= 0 ? 1L : query.getPageNum();
        long pageSize = query == null || query.getPageSize() <= 0 ? 10L : query.getPageSize();
        int fromIndex = (int) Math.min((pageNum - 1) * pageSize, total);
        int toIndex = (int) Math.min(fromIndex + pageSize, total);
        List<SamplingTask> pageRecords = total == 0L
                ? Collections.emptyList()
                : new ArrayList<>(records.subList(fromIndex, toIndex));
        prepareTasksForView(pageRecords);
        return new PageResult<>(total, pageRecords);
    }

    private List<StatusCountVO> todoStatusStats(SamplingTaskQuery query) {
        List<SamplingTask> records = loadTodoTasks(query, true);
        long pendingCount = records.stream()
                .filter(task -> !isTaskRegistered(task))
                .count();
        long unloggedCount = records.stream()
                .filter(task -> !isTaskRegistered(task))
                .count();
        long todoCount = pendingCount;
        return Arrays.asList(
                statusCount("TODO", todoCount),
                statusCount("ALL", todoCount),
                statusCount(LabWorkflowConstants.SamplingTaskStatus.PENDING, pendingCount),
                statusCount("UNSAMPLED", pendingCount),
                statusCount("UNLOGGED", unloggedCount)
        );
    }

    private List<SamplingTask> loadTodoTasks(SamplingTaskQuery query, boolean ignoreTaskStatus) {
        String keyword = query == null ? null : StrUtil.trim(query.getKeyword());
        String taskStatus = query == null ? null : StrUtil.trim(query.getTaskStatus());
        String sampleRegisterStatus = query == null ? null : StrUtil.trim(query.getSampleRegisterStatus());
        Long orgId = query == null ? null : query.getOrgId();
        Long scopedSamplerId = resolveScopedSamplerId(query == null ? null : query.getSamplerId());
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .and(StrUtil.isNotBlank(keyword), item -> item
                        .like(SamplingTask::getTaskNo, keyword)
                        .or()
                        .like(SamplingTask::getPointName, keyword)
                        .or()
                        .like(SamplingTask::getSampleNo, keyword))
                .in(SamplingTask::getTaskStatus,
                        LabWorkflowConstants.SamplingTaskStatus.PENDING,
                        LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS,
                        LabWorkflowConstants.SamplingTaskStatus.COMPLETED)
                .orderByDesc(SamplingTask::getCreatedTime);
        wrapper.eq(orgId != null, SamplingTask::getOrgId, orgId);
        if (!ignoreTaskStatus) {
            applyTaskStatusFilter(wrapper, taskStatus);
        }
        applySampleRegisterStatusFilter(wrapper, sampleRegisterStatus);
        applyTaskSamplerScope(wrapper, scopedSamplerId);
        List<SamplingTask> candidates = samplingTaskMapper.selectList(wrapper);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, LabSample> sampleMap = loadSampleMapByTaskIds(candidates);
        return candidates.stream()
                .filter(task -> shouldShowTodoTask(task, sampleMap.get(task.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, LabSample> loadSampleMapByTaskIds(List<SamplingTask> tasks) {
        List<Long> taskIds = tasks.stream()
                .map(SamplingTask::getId)
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                        .in(LabSample::getTaskId, taskIds))
                .stream()
                .filter(sample -> sample.getTaskId() != null)
                .collect(Collectors.toMap(LabSample::getTaskId, sample -> sample, (left, right) -> left, LinkedHashMap::new));
    }

    private boolean shouldShowTodoTask(SamplingTask task, LabSample sample) {
        if (task == null) {
            return false;
        }
        return !LabWorkflowConstants.SamplingTaskStatus.ABANDONED.equals(task.getTaskStatus())
                && !isTaskRegistered(task)
                && sample == null;
    }

    public SamplingTask detail(Long id) {
        SamplingTask task = requireTask(id);
        prepareTasksForView(Collections.singletonList(task));
        return task;
    }

    public List<SamplingTask> todoMine() {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        LambdaQueryWrapper<SamplingTask> wrapper = new LambdaQueryWrapper<SamplingTask>()
                .in(SamplingTask::getTaskStatus, LabWorkflowConstants.TODO_TASK_STATUSES);
        applyTaskSamplerScope(wrapper, currentUser.getUserId());
        wrapper.orderByAsc(SamplingTask::getSamplingTime);
        List<SamplingTask> tasks = samplingTaskMapper.selectList(wrapper);
        prepareTasksForView(tasks);
        return tasks;
    }

    public void start(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canStartTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许开始执行。");
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.IN_PROGRESS);
        task.setStartedTime(LocalDateTime.now());
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    public void abandon(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canAbandonTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务状态不允许废弃。");
        }
        String reason = command == null ? null : StrUtil.trim(command.getReason());
        if (StrUtil.isBlank(reason)) {
            throw new BusinessException("废弃任务时必须填写原因。");
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.ABANDONED);
        task.setAbandonReason(reason);
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    public void resume(Long taskId, SamplingTaskActionCommand command) {
        SamplingTask task = requireTask(taskId);
        validateTaskOperator(task);
        if (!LabWorkflowConstants.canResumeTask(task.getTaskStatus())) {
            throw new BusinessException("当前任务不是废弃状态，不能恢复。");
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.PENDING);
        task.setAbandonReason(null);
        if (command != null && StrUtil.isNotBlank(command.getRemark())) {
            task.setRemark(command.getRemark());
        }
        samplingTaskMapper.updateById(task);
        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(SamplingTaskCompleteCommand command) {
        SamplingTask task = requireTask(command.getTaskId());
        validateTaskOperator(task);
        if (LabWorkflowConstants.SamplingTaskStatus.ABANDONED.equals(task.getTaskStatus())) {
            throw new BusinessException("已废弃的任务不能直接完成。");
        }
        if (LabWorkflowConstants.SamplingTaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            throw new BusinessException("当前任务已经完成。");
        }
        if (!LabWorkflowConstants.canCompleteTask(task.getTaskStatus())) {
            throw new BusinessException("请先开始采样任务，再提交完成。");
        }

        task.setSampleNo(sampleNoGeneratorService.ensureSampleNo(task.getSampleNo()));
        task.setOnsiteMetrics(null);
        task.setWeather(command.getWeather());
        task.setTemperature(command.getTemperature());
        task.setSampleTotalVolume(StrUtil.trim(command.getSampleTotalVolume()));
        task.setSampleBottleCount(StrUtil.trim(command.getSampleBottleCount()));
        task.setPhotoUrls(normalizePhotoUrls(command.getPhotoUrls()));
        task.setRemark(command.getRemark());
        if (StrUtil.isNotBlank(command.getAddress())) {
            task.setAddress(StrUtil.trim(command.getAddress()));
        }
        if (StrUtil.isNotBlank(command.getLatitude())) {
            task.setLatitude(StrUtil.trim(command.getLatitude()));
        }
        if (StrUtil.isNotBlank(command.getLongitude())) {
            task.setLongitude(StrUtil.trim(command.getLongitude()));
        }
        if (task.getStartedTime() == null) {
            task.setStartedTime(LocalDateTime.now());
        }
        task.setTaskStatus(LabWorkflowConstants.SamplingTaskStatus.COMPLETED);
        task.setFinishedTime(LocalDateTime.now());
        samplingTaskMapper.updateById(task);

        samplingPlanService.refreshPlanStatusAfterTaskChange(task.getPlanId());
    }

    private String normalizePhotoUrls(String photoUrls) {
        if (StrUtil.isBlank(photoUrls)) {
            return null;
        }
        return Arrays.stream(photoUrls.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .map(storageService::toFullUrl)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private void normalizeTaskFileUrlsForView(SamplingTask task) {
        if (task != null) {
            task.setPhotoUrls(storageService.toFullUrls(task.getPhotoUrls()));
        }
    }

    private void prepareTasksForView(List<SamplingTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        fillTaskSampleContext(tasks);
        tasks.forEach(this::normalizeTaskFileUrlsForView);
        tasks.forEach(this::enrichTaskDetectionConfigSnapshotForView);
    }

    private void fillTaskSampleContext(List<SamplingTask> tasks) {
        Map<Long, LabSample> sampleMap = loadSampleMapByTaskIds(tasks);
        Map<Long, SamplingPlan> planMap = loadPlanMap(tasks);
        for (SamplingTask task : tasks) {
            LabSample sample = sampleMap.get(task.getId());
            SamplingPlan plan = task.getPlanId() == null ? null : planMap.get(task.getPlanId());
            if (sample != null) {
                fillBlankTaskSampleContext(task, sample.getPointName(), sample.getSampleType());
            }
            if (plan != null) {
                fillBlankTaskSampleContext(task, plan.getPointName(), plan.getSampleType());
                if (StrUtil.isNotBlank(plan.getPlanName())) {
                    task.setPlanName(plan.getPlanName());
                }
            }
        }
    }

    private Map<Long, SamplingPlan> loadPlanMap(List<SamplingTask> tasks) {
        List<Long> planIds = tasks.stream()
                .map(SamplingTask::getPlanId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return samplingPlanMapper.selectList(new LambdaQueryWrapper<SamplingPlan>()
                        .in(SamplingPlan::getId, planIds))
                .stream()
                .collect(Collectors.toMap(SamplingPlan::getId, plan -> plan, (left, right) -> left, LinkedHashMap::new));
    }

    private void fillBlankTaskSampleContext(SamplingTask task, String pointName, String sampleType) {
        if (task == null) {
            return;
        }
        if (StrUtil.isBlank(task.getPointName()) && StrUtil.isNotBlank(pointName)) {
            task.setPointName(pointName);
        }
        if (StrUtil.isBlank(task.getSampleType()) && StrUtil.isNotBlank(sampleType)) {
            task.setSampleType(sampleType);
        }
    }

    private void enrichTaskDetectionConfigSnapshotForView(SamplingTask task) {
        if (task != null) {
            task.setDetectionConfigSnapshot(samplingPlanService.enrichDetectionConfigSnapshotForView(task.getDetectionConfigSnapshot()));
        }
    }

    private SamplingTask requireTask(Long id) {
        SamplingTask task = samplingTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("采样任务不存在。");
        }
        return task;
    }

    private void validateTaskOperator(SamplingTask task) {
        CurrentUser currentUser = SecurityContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("当前登录信息已失效，请重新登录。");
        }
        if (isAdmin(currentUser)) {
            return;
        }
        if (!isSamplerAssigned(task, currentUser.getUserId())) {
            throw new BusinessException("当前用户不是该采样任务的责任采样员，不能执行此操作。");
        }
    }

    private void applyTaskSamplerScope(LambdaQueryWrapper<SamplingTask> wrapper, Long samplerId) {
        if (wrapper == null || samplerId == null) {
            return;
        }
        wrapper.and(item -> item
                .eq(SamplingTask::getSamplerId, samplerId)
                .or()
                .eq(SamplingTask::getSamplerIds, String.valueOf(samplerId))
                .or()
                .like(SamplingTask::getSamplerIds, wrapSamplerId(samplerId))
                .or()
                .likeRight(SamplingTask::getSamplerIds, samplerId + ",")
                .or()
                .likeLeft(SamplingTask::getSamplerIds, "," + samplerId));
    }

    private void applyTaskStatusFilter(LambdaQueryWrapper<SamplingTask> wrapper, String taskStatus) {
        if (wrapper == null || StrUtil.isBlank(taskStatus)) {
            return;
        }
        List<String> statuses = Arrays.stream(taskStatus.split(","))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (statuses.isEmpty()) {
            return;
        }
        if (statuses.size() == 1) {
            wrapper.eq(SamplingTask::getTaskStatus, statuses.get(0));
            return;
        }
        wrapper.in(SamplingTask::getTaskStatus, statuses);
    }

    private void applySampleRegisterStatusFilter(LambdaQueryWrapper<SamplingTask> wrapper, String sampleRegisterStatus) {
        if (wrapper == null || StrUtil.isBlank(sampleRegisterStatus)) {
            return;
        }
        if (LabWorkflowConstants.SampleRegisterStatus.UNREGISTERED.equals(sampleRegisterStatus)) {
            wrapper.ne(SamplingTask::getTaskStatus, LabWorkflowConstants.SamplingTaskStatus.ABANDONED)
                    .isNull(SamplingTask::getSampleId)
                    .and(item -> item
                            .isNull(SamplingTask::getSampleRegisterStatus)
                            .or()
                            .eq(SamplingTask::getSampleRegisterStatus, LabWorkflowConstants.SampleRegisterStatus.UNREGISTERED));
            return;
        }
        if (LabWorkflowConstants.SampleRegisterStatus.REGISTERED.equals(sampleRegisterStatus)) {
            wrapper.and(item -> item
                    .eq(SamplingTask::getSampleRegisterStatus, LabWorkflowConstants.SampleRegisterStatus.REGISTERED)
                    .or()
                    .isNotNull(SamplingTask::getSampleId));
            return;
        }
        wrapper.eq(SamplingTask::getSampleRegisterStatus, sampleRegisterStatus);
    }

    private boolean isSamplerAssigned(SamplingTask task, Long samplerId) {
        if (task == null || samplerId == null) {
            return false;
        }
        if (samplerId.equals(task.getSamplerId())) {
            return true;
        }
        return containsSamplerId(task.getSamplerIds(), samplerId);
    }

    private boolean containsSamplerId(String samplerIds, Long samplerId) {
        if (StrUtil.isBlank(samplerIds) || samplerId == null) {
            return false;
        }
        String target = String.valueOf(samplerId);
        if (StrUtil.equals(StrUtil.trim(samplerIds), target)
                || StrUtil.contains(samplerIds, wrapSamplerId(samplerId))) {
            return true;
        }
        String normalized = samplerIds
                .replace("[", ",")
                .replace("]", ",")
                .replace("\"", "")
                .replace("'", "");
        return Arrays.stream(normalized.split(","))
                .map(StrUtil::trim)
                .anyMatch(target::equals);
    }

    private String wrapSamplerId(Long samplerId) {
        return samplerId == null ? null : "," + samplerId + ",";
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser != null
                && ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())
                || "DIRECTOR".equalsIgnoreCase(currentUser.getRoleCode()));
    }

    private boolean isTaskRegistered(SamplingTask task) {
        if (task == null) {
            return false;
        }
        if (task.getSampleId() != null
                || LabWorkflowConstants.SampleRegisterStatus.REGISTERED.equals(task.getSampleRegisterStatus())) {
            return true;
        }
        Long sampleCount = labSampleMapper.selectCount(new LambdaQueryWrapper<LabSample>()
                .eq(LabSample::getTaskId, task.getId()));
        return sampleCount != null && sampleCount.longValue() > 0;
    }
}
