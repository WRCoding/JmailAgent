package com.longjunwang.jmailagent.util;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {
    // 使用 AtomicLong 保证线程安全
    private static final AtomicLong lastCallTimestamp = new AtomicLong(0);
    private static final Long interval = 12_000L;

    /**
     * 检查是否可以调用 API 并进行限流
     */
    public static void limit() {
        long currentTimestamp = System.currentTimeMillis();

        // 获取上一次调用的时间戳
        long lastCall = lastCallTimestamp.get();

        // 如果上一次调用距离现在不足 12 秒，则等待直到 12 秒过去
        if (currentTimestamp - lastCall < interval) {
            try {
                long timeToWait = interval - (currentTimestamp - lastCall);
                TimeUnit.MILLISECONDS.sleep(timeToWait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for rate limit", e);
            }
        }

        // 更新最后一次调用的时间戳
        lastCallTimestamp.set(currentTimestamp);

    }
}