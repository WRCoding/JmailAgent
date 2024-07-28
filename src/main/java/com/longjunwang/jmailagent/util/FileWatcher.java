package com.longjunwang.jmailagent.util;

import cn.hutool.core.io.watch.SimpleWatcher;
import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.model.PutObjectResult;
import com.longjunwang.jmailagent.browser.BrowserService;
import com.longjunwang.jmailagent.entity.InvoiceInfo;
import com.longjunwang.jmailagent.service.InvoiceService;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FileWatcher extends SimpleWatcher {
    private static final AtomicInteger count = new AtomicInteger(0);
    private static final InvoiceService invoiceService = SpringUtil.getBean(InvoiceService.class);
    @Override
    public void onCreate(WatchEvent<?> event, Path currentPath) {
        Object obj = event.context();
        if (Objects.isNull(obj)){
            return;
        }
        //避免文件创建了,但还没有完全写入
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ignore) {
        }
        String filePath = currentPath.toString() + File.separator + obj;
        File file = new File(filePath);
        if (checkFileExist(file)){
            log.info("file exist: {}, fileName: {}, size: {}",file.exists(), file.getName(), file.length());
            if (filePath.endsWith(".pdf")){
                InvoiceInfo invoiceInfo = TencentUtil.ocr_invoice(file);
                if (Objects.nonNull(invoiceInfo)){
                    PutObjectResult result = OssUtil.upload(file);
                    if (Objects.nonNull(result)) {
                        invoiceService.insert(invoiceInfo);
                    }else{
                        log.info("PutObjectResult null: {}", filePath);
                    }
                }else{
                    log.info("invoiceInfo null: {}", filePath);
                }
                log.info("count: {}, 创建：{}-> {}", count.incrementAndGet(), currentPath, obj);
            }
        }
    }

    /**
     * 检查文件是否存在,循环60次,每次休眠一秒,存在直接返回
     * @param file
     * @return
     */
    private boolean checkFileExist(File file) {
        for (int i = 0; i < 60; i++) {
            if (file.exists()) {
                return true;
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ignore) {
            }
        }
        return false;
    }
}
