package com.longjunwang.jmailagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {"com.longjunwang.jmailagent.mapper"})
public class JmailAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmailAgentApplication.class, args);
    }

}
