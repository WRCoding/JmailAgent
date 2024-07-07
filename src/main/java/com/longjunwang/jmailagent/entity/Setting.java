package com.longjunwang.jmailagent.entity;

import lombok.Data;

@Data
public class Setting {
    private String since;
    private String subject;
    private Integer lastEmailId;
}
