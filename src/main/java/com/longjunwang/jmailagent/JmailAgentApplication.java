package com.longjunwang.jmailagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(basePackages = {"com.longjunwang.jmailagent.mapper"})
@ComponentScan(basePackages = {"com.longjunwang.jmailagent.util","com.longjunwang.jmailagent"})
public class JmailAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(JmailAgentApplication.class, args);
    }

}
