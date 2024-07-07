package com.longjunwang.jmailagent.mapper;

import com.longjunwang.jmailagent.entity.FailUrl;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FailUrlMapper {

    void insert(FailUrl failUrl);
}
