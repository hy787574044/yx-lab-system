package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yx.lab.modules.detection.entity.DetectionRecord;
import com.yx.lab.modules.detection.mapper.DetectionRecordMapper;
import com.yx.lab.modules.report.entity.ReportPushRecord;
import com.yx.lab.modules.report.mapper.ReportPushRecordMapper;
import com.yx.lab.modules.review.entity.ReviewRecord;
import com.yx.lab.modules.review.mapper.ReviewRecordMapper;
import com.yx.lab.modules.sample.entity.LabSample;
import com.yx.lab.modules.sample.mapper.LabSampleMapper;
import com.yx.lab.modules.system.dto.LogQuery;
import com.yx.lab.modules.system.entity.LabLoginLog;
import com.yx.lab.modules.system.mapper.LabLoginLogMapper;
import com.yx.lab.modules.system.vo.SystemLogPageVO;
import com.yx.lab.modules.system.vo.SystemLogVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 系统日志管理服务。
 */
@Service
@RequiredArgsConstructor
public class SystemLogManagementService {

    private static final String SOURCE_TYPE_LOGIN = "LOGIN";
    private static final String SOURCE_TYPE_PROCESS = "PROCESS";
    private static final String SOURCE_TYPE_SAMPLE = "SAMPLE";
    private static final String SOURCE_TYPE_DETECTION = "DETECTION";
    private static final String SOURCE_TYPE_REVIEW = "REVIEW";
    private static final String SOURCE_TYPE_PUSH = "PUSH";

    private static final Pattern TRACE_LOG_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\[(.+?)] (.+)$");
    private static final DateTimeFormatter TRACE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LabLoginLogMapper labLoginLogMapper;
    private final LabSampleMapper labSampleMapper;
    private final DetectionRecordMapper detectionRecordMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReportPushRecordMapper reportPushRecordMapper;

    /**
     * 分页查询系统日志。
     *
     * @param query 查询条件
     * @return 统一日志分页结果
     */
    public SystemLogPageVO page(LogQuery query) {
        String keyword = normalizeKeyword(query.getKeyword());
        String sourceType = normalizeSourceType(query.getSourceType());

        List<SystemLogVO> allLogs = new ArrayList<>();
        allLogs.addAll(buildLoginLogs());
        allLogs.addAll(buildSampleTraceLogs());
        allLogs.addAll(buildDetectionLogs());
        allLogs.addAll(buildReviewLogs());
        allLogs.addAll(buildPushLogs());

        List<SystemLogVO> keywordMatchedLogs = allLogs.stream()
                .filter(item -> matchesKeyword(item, keyword))
                .sorted(Comparator
                        .comparing(SystemLogVO::getOccurTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SystemLogVO::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long loginCount = keywordMatchedLogs.stream()
                .filter(item -> SOURCE_TYPE_LOGIN.equals(item.getSourceType()))
                .count();
        long processCount = keywordMatchedLogs.stream()
                .filter(item -> isProcessSourceType(item.getSourceType()))
                .count();
        long pushCount = keywordMatchedLogs.stream()
                .filter(item -> SOURCE_TYPE_PUSH.equals(item.getSourceType()))
                .count();

        List<SystemLogVO> filteredLogs = keywordMatchedLogs.stream()
                .filter(item -> matchesSourceType(item, sourceType))
                .collect(Collectors.toList());

        long total = filteredLogs.size();
        List<SystemLogVO> pageRecords = paginate(filteredLogs, query.getPageNum(), query.getPageSize());
        return new SystemLogPageVO(total, pageRecords, keywordMatchedLogs.size(), loginCount, processCount, pushCount);
    }

    private List<SystemLogVO> buildLoginLogs() {
        return labLoginLogMapper.selectList(new LambdaQueryWrapper<LabLoginLog>()
                        .orderByDesc(LabLoginLog::getLoginTime)
                        .orderByDesc(LabLoginLog::getCreatedTime))
                .stream()
                .map(this::toLoginLogVO)
                .collect(Collectors.toList());
    }

    private List<SystemLogVO> buildSampleTraceLogs() {
        List<SystemLogVO> logs = new ArrayList<>();
        List<LabSample> samples = labSampleMapper.selectList(new LambdaQueryWrapper<LabSample>()
                .isNotNull(LabSample::getTraceLog)
                .orderByDesc(LabSample::getUpdatedTime)
                .orderByDesc(LabSample::getCreatedTime));
        for (LabSample sample : samples) {
            if (StrUtil.isBlank(sample.getTraceLog())) {
                continue;
            }
            List<String> lines = StrUtil.splitTrim(sample.getTraceLog(), '\n');
            for (int index = 0; index < lines.size(); index++) {
                String line = StrUtil.trim(lines.get(index));
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                logs.add(toSampleTraceLogVO(sample, line, index));
            }
        }
        return logs;
    }

    private List<SystemLogVO> buildDetectionLogs() {
        return detectionRecordMapper.selectList(new LambdaQueryWrapper<DetectionRecord>()
                        .orderByDesc(DetectionRecord::getDetectionTime)
                        .orderByDesc(DetectionRecord::getCreatedTime))
                .stream()
                .map(this::toDetectionLogVO)
                .collect(Collectors.toList());
    }

    private List<SystemLogVO> buildReviewLogs() {
        return reviewRecordMapper.selectList(new LambdaQueryWrapper<ReviewRecord>()
                        .orderByDesc(ReviewRecord::getReviewTime)
                        .orderByDesc(ReviewRecord::getCreatedTime))
                .stream()
                .map(this::toReviewLogVO)
                .collect(Collectors.toList());
    }

    private List<SystemLogVO> buildPushLogs() {
        return reportPushRecordMapper.selectList(new LambdaQueryWrapper<ReportPushRecord>()
                        .orderByDesc(ReportPushRecord::getPushTime)
                        .orderByDesc(ReportPushRecord::getCreatedTime))
                .stream()
                .map(this::toPushLogVO)
                .collect(Collectors.toList());
    }

    private SystemLogVO toLoginLogVO(LabLoginLog log) {
        SystemLogVO vo = new SystemLogVO();
        vo.setId(SOURCE_TYPE_LOGIN + "-" + log.getId());
        vo.setSourceType(SOURCE_TYPE_LOGIN);
        vo.setSourceName("登录认证");
        vo.setBusinessNo(normalizeDisplayValue(log.getUsername()));
        vo.setOperatorName(resolveOperatorName(log.getRealName(), log.getUsername()));
        vo.setContent(resolveLoginChannelLabel(log.getLoginChannel()) + "端登录成功");
        vo.setStatus(resolveLoginStatusLabel(log.getLoginStatus()));
        vo.setOccurTime(firstNotNull(log.getLoginTime(), log.getCreatedTime(), log.getUpdatedTime()));
        return vo;
    }

    private SystemLogVO toSampleTraceLogVO(LabSample sample, String line, int index) {
        TraceLogEntry entry = parseTraceLog(line);
        SystemLogVO vo = new SystemLogVO();
        vo.setId(SOURCE_TYPE_SAMPLE + "-" + sample.getId() + "-" + index);
        vo.setSourceType(SOURCE_TYPE_SAMPLE);
        vo.setSourceName("样品留痕");
        vo.setBusinessNo(buildSampleBusinessNo(sample.getSampleNo(), sample.getSealNo()));
        vo.setOperatorName(resolveTraceOperator(entry.getContent(), sample.getSamplerName(), sample.getUpdatedName(), sample.getCreatedName()));
        vo.setContent(entry.getTitle() + "：" + entry.getContent());
        vo.setStatus("已留痕");
        vo.setOccurTime(firstNotNull(entry.getOccurTime(), sample.getUpdatedTime(), sample.getCreatedTime()));
        return vo;
    }

    private SystemLogVO toDetectionLogVO(DetectionRecord record) {
        SystemLogVO vo = new SystemLogVO();
        vo.setId(SOURCE_TYPE_DETECTION + "-" + record.getId());
        vo.setSourceType(SOURCE_TYPE_DETECTION);
        vo.setSourceName("检测记录");
        vo.setBusinessNo(buildSampleBusinessNo(record.getSampleNo(), record.getSealNo()));
        vo.setOperatorName(normalizeDisplayValue(record.getDetectorName()));
        vo.setContent(buildDetectionContent(record));
        vo.setStatus(resolveDetectionStatusLabel(record.getDetectionStatus()));
        vo.setOccurTime(firstNotNull(record.getDetectionTime(), record.getCreatedTime(), record.getUpdatedTime()));
        return vo;
    }

    private SystemLogVO toReviewLogVO(ReviewRecord record) {
        SystemLogVO vo = new SystemLogVO();
        vo.setId(SOURCE_TYPE_REVIEW + "-" + record.getId());
        vo.setSourceType(SOURCE_TYPE_REVIEW);
        vo.setSourceName("审核记录");
        vo.setBusinessNo(buildSampleBusinessNo(record.getSampleNo(), record.getSealNo()));
        vo.setOperatorName(normalizeDisplayValue(record.getReviewerName()));
        vo.setContent(buildReviewContent(record));
        vo.setStatus(resolveReviewResultLabel(record.getReviewResult()));
        vo.setOccurTime(firstNotNull(record.getReviewTime(), record.getCreatedTime(), record.getUpdatedTime()));
        return vo;
    }

    private SystemLogVO toPushLogVO(ReportPushRecord record) {
        SystemLogVO vo = new SystemLogVO();
        vo.setId(SOURCE_TYPE_PUSH + "-" + record.getId());
        vo.setSourceType(SOURCE_TYPE_PUSH);
        vo.setSourceName("报告推送");
        vo.setBusinessNo(buildSampleBusinessNo(record.getSampleNo(), record.getSealNo()));
        vo.setOperatorName(normalizeDisplayValue(record.getRecipientName()));
        vo.setContent(buildPushContent(record));
        vo.setStatus(resolvePushStatusLabel(record.getPushStatus()));
        vo.setOccurTime(firstNotNull(record.getPushTime(), record.getCreatedTime(), record.getUpdatedTime()));
        return vo;
    }

    private String buildSampleBusinessNo(String sampleNo, String sealNo) {
        if (StrUtil.isNotBlank(sampleNo) && StrUtil.isNotBlank(sealNo)) {
            return sampleNo + " / " + sealNo;
        }
        if (StrUtil.isNotBlank(sampleNo)) {
            return sampleNo;
        }
        if (StrUtil.isNotBlank(sealNo)) {
            return sealNo;
        }
        return "-";
    }

    private String buildDetectionContent(DetectionRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append("检测类型=").append(normalizeDisplayValue(record.getDetectionTypeName()));
        builder.append("，检测结果=").append(resolveDetectionResultLabel(record.getDetectionResult()));
        if (StrUtil.isNotBlank(record.getAbnormalRemark())) {
            builder.append("，异常说明=").append(StrUtil.trim(record.getAbnormalRemark()));
        }
        return builder.toString();
    }

    private String buildReviewContent(ReviewRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append("审核结果=").append(resolveReviewResultLabel(record.getReviewResult()));
        if (StrUtil.isNotBlank(record.getRejectReason())) {
            builder.append("，驳回原因=").append(StrUtil.trim(record.getRejectReason()));
        }
        if (StrUtil.isNotBlank(record.getReviewRemark())) {
            builder.append("，审核备注=").append(StrUtil.trim(record.getReviewRemark()));
        }
        return builder.toString();
    }

    private String buildPushContent(ReportPushRecord record) {
        StringBuilder builder = new StringBuilder();
        builder.append("推送渠道=").append(resolvePushChannelLabel(record.getPushChannel()));
        builder.append("，接收人=").append(normalizeDisplayValue(record.getRecipientName()));
        if (StrUtil.isNotBlank(record.getRecipientPhone())) {
            builder.append("，接收手机号=").append(StrUtil.trim(record.getRecipientPhone()));
        }
        if (StrUtil.isNotBlank(record.getPushMessage())) {
            builder.append("，结果说明=").append(StrUtil.trim(record.getPushMessage()));
        }
        return builder.toString();
    }

    private TraceLogEntry parseTraceLog(String line) {
        Matcher matcher = TRACE_LOG_PATTERN.matcher(StrUtil.blankToDefault(line, ""));
        if (!matcher.matches()) {
            return new TraceLogEntry("流程留痕", normalizeDisplayValue(line), null);
        }
        LocalDateTime occurTime = null;
        try {
            occurTime = LocalDateTime.parse(matcher.group(1), TRACE_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
        }
        return new TraceLogEntry(
                normalizeDisplayValue(matcher.group(2)),
                normalizeDisplayValue(matcher.group(3)),
                occurTime);
    }

    private String resolveTraceOperator(String content, String... fallbackNames) {
        String[] candidateKeys = {"采样人=", "检测人=", "审核人=", "发布人=", "接收人="};
        for (String candidateKey : candidateKeys) {
            String value = extractOperatorFromContent(content, candidateKey);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        for (String fallbackName : fallbackNames) {
            if (StrUtil.isNotBlank(fallbackName)) {
                return StrUtil.trim(fallbackName);
            }
        }
        return "-";
    }

    private String extractOperatorFromContent(String content, String key) {
        if (StrUtil.isBlank(content) || StrUtil.isBlank(key)) {
            return null;
        }
        int startIndex = content.indexOf(key);
        if (startIndex < 0) {
            return null;
        }
        String remaining = content.substring(startIndex + key.length());
        int endIndex = remaining.indexOf('，');
        if (endIndex < 0) {
            endIndex = remaining.indexOf(',');
        }
        String operator = endIndex < 0 ? remaining : remaining.substring(0, endIndex);
        return StrUtil.trim(operator);
    }

    private List<SystemLogVO> paginate(List<SystemLogVO> logs, long pageNum, long pageSize) {
        if (logs.isEmpty()) {
            return new ArrayList<>();
        }
        long safePageNum = pageNum <= 0 ? 1L : pageNum;
        long safePageSize = pageSize <= 0 ? 10L : pageSize;
        long startIndex = (safePageNum - 1L) * safePageSize;
        if (startIndex >= logs.size()) {
            return new ArrayList<>();
        }
        long endIndex = Math.min(startIndex + safePageSize, logs.size());
        return new ArrayList<>(logs.subList((int) startIndex, (int) endIndex));
    }

    private boolean matchesKeyword(SystemLogVO item, String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return true;
        }
        return containsIgnoreCase(item.getSourceName(), keyword)
                || containsIgnoreCase(item.getBusinessNo(), keyword)
                || containsIgnoreCase(item.getOperatorName(), keyword)
                || containsIgnoreCase(item.getContent(), keyword)
                || containsIgnoreCase(item.getStatus(), keyword);
    }

    private boolean matchesSourceType(SystemLogVO item, String sourceType) {
        if (StrUtil.isBlank(sourceType)) {
            return true;
        }
        if (SOURCE_TYPE_PROCESS.equals(sourceType)) {
            return isProcessSourceType(item.getSourceType());
        }
        return StrUtil.equals(sourceType, item.getSourceType());
    }

    private boolean isProcessSourceType(String sourceType) {
        return SOURCE_TYPE_SAMPLE.equals(sourceType)
                || SOURCE_TYPE_DETECTION.equals(sourceType)
                || SOURCE_TYPE_REVIEW.equals(sourceType);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StrUtil.containsIgnoreCase(StrUtil.blankToDefault(value, ""), keyword);
    }

    private String normalizeKeyword(String keyword) {
        return StrUtil.trim(StrUtil.blankToDefault(keyword, ""));
    }

    private String normalizeSourceType(String sourceType) {
        return StrUtil.trim(StrUtil.blankToDefault(sourceType, "")).toUpperCase(Locale.ROOT);
    }

    private String resolveOperatorName(String realName, String username) {
        if (StrUtil.isNotBlank(realName)) {
            return StrUtil.trim(realName);
        }
        return normalizeDisplayValue(username);
    }

    private String resolveLoginChannelLabel(String loginChannel) {
        if ("MOBILE".equalsIgnoreCase(loginChannel)) {
            return "移动";
        }
        if ("PC".equalsIgnoreCase(loginChannel)) {
            return "PC";
        }
        return "未知";
    }

    private String resolveLoginStatusLabel(String loginStatus) {
        if ("SUCCESS".equalsIgnoreCase(loginStatus)) {
            return "成功";
        }
        if ("FAILED".equalsIgnoreCase(loginStatus)) {
            return "失败";
        }
        return normalizeDisplayValue(loginStatus);
    }

    private String resolveDetectionStatusLabel(String detectionStatus) {
        if ("SUBMITTED".equalsIgnoreCase(detectionStatus)) {
            return "待审核";
        }
        if ("APPROVED".equalsIgnoreCase(detectionStatus)) {
            return "已通过";
        }
        if ("REJECTED".equalsIgnoreCase(detectionStatus)) {
            return "已驳回";
        }
        return normalizeDisplayValue(detectionStatus);
    }

    private String resolveDetectionResultLabel(String detectionResult) {
        if ("NORMAL".equalsIgnoreCase(detectionResult)) {
            return "正常";
        }
        if ("ABNORMAL".equalsIgnoreCase(detectionResult)) {
            return "异常";
        }
        return normalizeDisplayValue(detectionResult);
    }

    private String resolveReviewResultLabel(String reviewResult) {
        if ("APPROVED".equalsIgnoreCase(reviewResult)) {
            return "审核通过";
        }
        if ("REJECTED".equalsIgnoreCase(reviewResult)) {
            return "审核驳回";
        }
        return normalizeDisplayValue(reviewResult);
    }

    private String resolvePushStatusLabel(String pushStatus) {
        if ("SUCCESS".equalsIgnoreCase(pushStatus)) {
            return "推送成功";
        }
        if ("FAILED".equalsIgnoreCase(pushStatus)) {
            return "推送失败";
        }
        if ("PENDING".equalsIgnoreCase(pushStatus)) {
            return "待推送";
        }
        if ("CANCELLED".equalsIgnoreCase(pushStatus)) {
            return "已取消";
        }
        return normalizeDisplayValue(pushStatus);
    }

    private String resolvePushChannelLabel(String pushChannel) {
        if ("SMS".equalsIgnoreCase(pushChannel)) {
            return "短信";
        }
        if ("WECHAT".equalsIgnoreCase(pushChannel)) {
            return "微信";
        }
        if ("APP".equalsIgnoreCase(pushChannel)) {
            return "移动端";
        }
        return normalizeDisplayValue(pushChannel);
    }

    private String normalizeDisplayValue(String value) {
        return StrUtil.isBlank(value) ? "-" : StrUtil.trim(value);
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (Objects.nonNull(value)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 留痕解析结果。
     */
    @Data
    @AllArgsConstructor
    private static class TraceLogEntry {

        private String title;
        private String content;
        private LocalDateTime occurTime;
    }
}
