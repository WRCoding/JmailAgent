package com.longjunwang.jmailagent.util;

import cn.hutool.core.io.watch.SimpleWatcher;
import com.longjunwang.jmailagent.entity.AttachEvent;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class AttachFileWatcher extends SimpleWatcher implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;
    private static final AtomicInteger count = new AtomicInteger(0);
    @Override
    public void onCreate(WatchEvent<?> event, Path currentPath) {
        Object obj = event.context();
        if (Objects.isNull(obj)){
            return;
        }
        String filePath = currentPath.toString() + File.separator + obj;
        File file = new File(filePath);
        if (waitForDownloadToComplete(file)){
            log.info("file exist: {}, fileName: {}, size: {}",file.exists(), file.getName(), file.length());
            if (filePath.endsWith(".pdf")){
                publisher.publishEvent(new AttachEvent("attach", file));
                log.info("count: {}, 创建：{}-> {}", count.incrementAndGet(), currentPath, obj);
            }
        }
    }

    private boolean waitForDownloadToComplete(File file) {
        try {
            long lastSize = -1;
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < TimeUnit.SECONDS.toMillis(65)) {
                if (file.exists()) {
                    long currentSize = file.length();
                    if (lastSize == currentSize && file.getName().endsWith(".pdf") && file.length() > 0) {
                        return true;
                    }
                    log.info("lastSize: {}, fileName: {}, currentSize: {}", lastSize, file.getName(), currentSize);
                    lastSize = currentSize;
                }
                Thread.sleep(1000); // 等待一段时间后再次检查
            }
            return false;
        } catch (InterruptedException e) {
            log.error("waitForDownloadToComplete error e: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void setApplicationEventPublisher(@NotNull ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
