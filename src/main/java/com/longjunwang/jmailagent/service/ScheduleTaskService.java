package com.longjunwang.jmailagent.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduleTaskService {

    @Resource
    private MailService mailService;

    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public void startTask() {
        executorService.scheduleAtFixedRate(() -> {
            mailService.invoke();
        }, 0, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public void stopTask(){
        executorService.shutdown();
    }
}
