package com.longjunwang.jmailagent.entity;

import lombok.Data;

@Data
public class FailUrl {
    private int id;
    private String url;
    private String create_time;

    public FailUrl() {
    }

    public FailUrl(String url) {
        this.url = url;
    }
}
