package com.yx.lab.modules.sample.service;

import cn.hutool.core.util.StrUtil;
import com.yx.lab.common.exception.BusinessException;
import com.yx.lab.modules.sample.mapper.SampleNoSequenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class SampleNoGeneratorService {

    private static final String PREFIX = "YX";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SampleNoSequenceMapper sampleNoSequenceMapper;

    @Transactional(rollbackFor = Exception.class)
    public String nextSampleNo() {
        String sequenceDate = LocalDate.now().format(DATE_FORMATTER);
        sampleNoSequenceMapper.nextValue(sequenceDate);
        Long nextValue = sampleNoSequenceMapper.lastInsertId();
        if (nextValue == null || nextValue <= 0) {
            throw new BusinessException("样品编号生成失败，请重试");
        }
        return PREFIX + sequenceDate + String.format("%04d", nextValue);
    }

    public String ensureSampleNo(String sampleNo) {
        String normalizedSampleNo = StrUtil.trim(sampleNo);
        return StrUtil.isNotBlank(normalizedSampleNo) ? normalizedSampleNo : nextSampleNo();
    }
}
