package com.yx.lab.modules.sample.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SampleNoSequenceMapper {

    @Insert("INSERT INTO lab_sample_no_sequence(sequence_date, current_value) " +
            "VALUES (#{sequenceDate}, LAST_INSERT_ID(1)) " +
            "ON DUPLICATE KEY UPDATE current_value = LAST_INSERT_ID(current_value + 1), updated_time = NOW()")
    int nextValue(@Param("sequenceDate") String sequenceDate);

    @Select("SELECT LAST_INSERT_ID()")
    Long lastInsertId();
}
