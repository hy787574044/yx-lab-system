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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典管理服务。
 */
@Service
@RequiredArgsConstructor
public class DictManagementService {

    private final LabDictMapper labDictMapper;

    /**
     * 分页查询数据字典。
     *
     * @param query 查询条件
     * @return 数据字典分页结果
     */
    public PageResult<LabDictVO> page(DictQuery query) {
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
}
