package com.yx.lab.modules.system.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.common.model.PageResult;
import com.yx.lab.common.util.PageUtils;
import com.yx.lab.modules.system.dto.DictQuery;
import com.yx.lab.modules.system.dto.DictSaveCommand;
import com.yx.lab.modules.system.entity.LabDict;
import com.yx.lab.modules.system.mapper.LabDictMapper;
import com.yx.lab.modules.system.vo.LabDictVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据字典管理服务。
 */
@Service
@RequiredArgsConstructor
public class DictManagementService {

    private static final List<BuiltInDictDefinition> BUILT_IN_DICTS = buildBuiltInDicts();

    private final LabDictMapper labDictMapper;

    /**
     * 分页查询数据字典。
     *
     * @param query 查询条件
     * @return 数据字典分页结果
     */
    public PageResult<LabDictVO> page(DictQuery query) {
        ensureBuiltInDicts();
        String keyword = StrUtil.trim(query.getKeyword());
        Page<LabDict> page = labDictMapper.selectPage(
                PageUtils.buildPage(query),
                new LambdaQueryWrapper<LabDict>()
                        .and(StrUtil.isNotBlank(keyword), wrapper -> wrapper
                                .like(LabDict::getDictCode, keyword)
                                .or()
                                .like(LabDict::getDictName, keyword)
                                .or()
                                .like(LabDict::getModuleName, keyword)
                                .or()
                                .like(LabDict::getItemText, keyword)
                                .or()
                                .like(LabDict::getRemark, keyword))
                        .eq(query.getStatus() != null, LabDict::getStatus, query.getStatus())
                        .orderByAsc(LabDict::getModuleName)
                        .orderByAsc(LabDict::getDictCode)
                        .orderByDesc(LabDict::getUpdatedTime));
        List<LabDictVO> records = page.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), records);
    }

    /**
     * 获取数据字典详情。
     *
     * @param id 字典ID
     * @return 数据字典详情
     */
    public LabDictVO detail(Long id) {
        return toVO(requireDict(id));
    }

    /**
     * 新增数据字典。
     *
     * @param command 保存命令
     */
    public void save(DictSaveCommand command) {
        validateDictCodeUnique(StrUtil.trim(command.getDictCode()), null);
        LabDict entity = new LabDict();
        applyCommand(entity, command);
        labDictMapper.insert(entity);
    }

    /**
     * 更新数据字典。
     *
     * @param id 字典ID
     * @param command 保存命令
     */
    public void update(Long id, DictSaveCommand command) {
        LabDict entity = requireDict(id);
        validateDictCodeUnique(StrUtil.trim(command.getDictCode()), id);
        applyCommand(entity, command);
        labDictMapper.updateById(entity);
    }

    /**
     * 删除数据字典。
     *
     * @param id 字典ID
     */
    public void delete(Long id) {
        LabDict entity = requireDict(id);
        labDictMapper.deleteById(entity.getId());
    }

    /**
     * 同步系统内置状态与业务字典。
     */
    public void ensureBuiltInDicts() {
        for (BuiltInDictDefinition definition : BUILT_IN_DICTS) {
            LabDict existing = labDictMapper.selectOne(new LambdaQueryWrapper<LabDict>()
                    .eq(LabDict::getDictCode, definition.dictCode)
                    .last("limit 1"));
            if (existing == null) {
                LabDict entity = new LabDict();
                applyBuiltInDefinition(entity, definition);
                labDictMapper.insert(entity);
                continue;
            }
            applyBuiltInDefinition(existing, definition);
            labDictMapper.updateById(existing);
        }
    }

    private LabDict requireDict(Long id) {
        LabDict entity = labDictMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("数据字典不存在");
        }
        return entity;
    }

    private void validateDictCodeUnique(String dictCode, Long excludeId) {
        Long count = labDictMapper.selectCount(new LambdaQueryWrapper<LabDict>()
                .eq(LabDict::getDictCode, dictCode)
                .ne(excludeId != null, LabDict::getId, excludeId));
        if (count != null && count > 0L) {
            throw new BusinessException("字典编码已存在");
        }
    }

    private void applyCommand(LabDict entity, DictSaveCommand command) {
        entity.setDictCode(StrUtil.trim(command.getDictCode()));
        entity.setDictName(StrUtil.trim(command.getDictName()));
        entity.setModuleName(StrUtil.trim(command.getModuleName()));
        entity.setItemText(StrUtil.trim(command.getItemText()));
        entity.setStatus(command.getStatus());
        entity.setRemark(StrUtil.trim(command.getRemark()));
    }

    private LabDictVO toVO(LabDict entity) {
        LabDictVO vo = new LabDictVO();
        vo.setId(entity.getId());
        vo.setDictCode(entity.getDictCode());
        vo.setDictName(entity.getDictName());
        vo.setModuleName(entity.getModuleName());
        vo.setItemCount(countItems(entity.getItemText()));
        vo.setItemText(entity.getItemText());
        vo.setStatus(entity.getStatus());
        vo.setRemark(entity.getRemark());
        vo.setUpdatedTime(entity.getUpdatedTime());
        return vo;
    }

    private Integer countItems(String itemText) {
        if (StrUtil.isBlank(itemText)) {
            return 0;
        }
        return (int) Arrays.stream(StrUtil.splitToArray(itemText, '\n'))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .count();
    }

    private void applyBuiltInDefinition(LabDict entity, BuiltInDictDefinition definition) {
        entity.setDictCode(definition.dictCode);
        entity.setDictName(definition.dictName);
        entity.setModuleName(definition.moduleName);
        entity.setItemText(definition.itemText);
        entity.setStatus(1);
        entity.setRemark(definition.remark);
    }

    private static List<BuiltInDictDefinition> buildBuiltInDicts() {
        List<BuiltInDictDefinition> definitions = new ArrayList<>();
        definitions.add(new BuiltInDictDefinition("instrument_status", "仪器状态", "设备管理",
                buildItemText(mapOf(
                        "NORMAL", "正常",
                        "DISABLED", "停用",
                        "MAINTENANCE", "维护中",
                        "CALIBRATING", "校准中"
                )),
                "系统内置：设备台账与设备相关流程统一使用。"));
        definitions.add(new BuiltInDictDefinition("point_status", "监测点状态", "采样管理",
                buildItemText(mapOf(
                        "ENABLED", "启用",
                        "DISABLED", "禁用"
                )),
                "系统内置：监测点位启停状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("point_type", "监测点类型", "采样管理",
                buildItemText(mapOf(
                        "FACTORY", "出厂水",
                        "RAW", "原水",
                        "TERMINAL", "管网末梢"
                )),
                "系统内置：监测点位类型统一使用。"));
        definitions.add(new BuiltInDictDefinition("frequency_type", "监测频次", "采样管理",
                buildItemText(mapOf(
                        "DAILY", "每日",
                        "WEEKLY", "每周",
                        "MONTHLY", "每月"
                )),
                "系统内置：点位监测频次统一使用。"));
        definitions.add(new BuiltInDictDefinition("cycle_type", "周期类型", "采样管理",
                buildItemText(mapOf(
                        "ONCE", "单次",
                        "DAILY", "每日",
                        "WEEKLY", "每周",
                        "MONTHLY", "每月"
                )),
                "系统内置：周期计划与自动任务周期统一使用。"));
        definitions.add(new BuiltInDictDefinition("sampling_type", "采样类型", "采样管理",
                buildItemText(mapOf(
                        "ROUTINE", "常规采样"
                )),
                "系统内置：采样任务类型统一使用。"));
        definitions.add(new BuiltInDictDefinition("sampling_plan_status", "采样计划状态", "采样管理",
                buildItemText(mapOf(
                        "ACTIVE", "启用中",
                        "PAUSED", "已暂停",
                        "DISPATCHED", "已派发",
                        "COMPLETED", "已完成",
                        "UNPUBLISHED", "待派发"
                )),
                "系统内置：周期计划与采样计划流程状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("sampling_task_status", "采样任务状态", "采样管理",
                buildItemText(mapOf(
                        "PENDING", "待处理",
                        "IN_PROGRESS", "进行中",
                        "ABANDONED", "已废弃",
                        "COMPLETED", "已完成"
                )),
                "系统内置：采样任务执行状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("sample_register_status", "样品登记状态", "采样管理",
                buildItemText(mapOf(
                        "UNREGISTERED", "未登记",
                        "REGISTERED", "已登记"
                )),
                "系统内置：采样任务对应样品登记状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("sample_type", "样品类型", "采样管理",
                buildItemText(mapOf(
                        "FACTORY", "出厂水",
                        "RAW", "原水",
                        "TERMINAL", "管网末梢"
                )),
                "系统内置：样品登录、样品台账、移动端统一使用。"));
        definitions.add(new BuiltInDictDefinition("sample_status", "样品状态", "采样管理",
                buildItemText(mapOf(
                        "LOGGED", "已登录",
                        "REVIEWING", "审核中",
                        "RETEST", "待重检",
                        "COMPLETED", "已完成"
                )),
                "系统内置：样品流程状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("detection_status", "检测流程状态", "检测管理",
                buildItemText(mapOf(
                        "WAIT_ASSIGN", "待分配",
                        "WAIT_DETECT", "待检测",
                        "SUBMITTED", "待审核",
                        "APPROVED", "审核通过",
                        "REJECTED", "审核驳回"
                )),
                "系统内置：检测主流程与参数子流程状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("detection_result", "检测结果", "检测管理",
                buildItemText(mapOf(
                        "NORMAL", "正常",
                        "ABNORMAL", "异常"
                )),
                "系统内置：检测结果判定统一使用。"));
        definitions.add(new BuiltInDictDefinition("review_result", "审核结果", "审核管理",
                buildItemText(mapOf(
                        "APPROVED", "审核通过",
                        "REJECTED", "审核驳回"
                )),
                "系统内置：结果审查结论统一使用。"));
        definitions.add(new BuiltInDictDefinition("report_type", "报告类型", "报告管理",
                buildItemText(mapOf(
                        "DAILY", "日报",
                        "WEEKLY", "周报",
                        "MONTHLY", "月报"
                )),
                "系统内置：报告类型统一使用。"));
        definitions.add(new BuiltInDictDefinition("report_status", "报告状态", "报告管理",
                buildItemText(mapOf(
                        "DRAFT", "草稿",
                        "GENERATED", "已生成",
                        "PUBLISHED", "已发布"
                )),
                "系统内置：报告正式产物状态统一使用。"));
        definitions.add(new BuiltInDictDefinition("report_push_status", "报告推送状态", "报告管理",
                buildItemText(mapOf(
                        "PENDING", "待推送",
                        "SUCCESS", "已推送",
                        "FAILED", "推送失败",
                        "CANCELLED", "已撤回"
                )),
                "系统内置：报告推送留痕状态统一使用。"));
        return definitions;
    }

    private static String buildItemText(Map<String, String> items) {
        return items.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private static Map<String, String> mapOf(String... keyValues) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            result.put(keyValues[index], keyValues[index + 1]);
        }
        return result;
    }

    private static class BuiltInDictDefinition {

        private final String dictCode;

        private final String dictName;

        private final String moduleName;

        private final String itemText;

        private final String remark;

        private BuiltInDictDefinition(String dictCode,
                                      String dictName,
                                      String moduleName,
                                      String itemText,
                                      String remark) {
            this.dictCode = dictCode;
            this.dictName = dictName;
            this.moduleName = moduleName;
            this.itemText = itemText;
            this.remark = remark;
        }
    }
}
