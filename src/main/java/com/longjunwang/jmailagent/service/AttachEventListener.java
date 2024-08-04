package com.longjunwang.jmailagent.service;

import com.longjunwang.jmailagent.entity.AttachEvent;
import com.longjunwang.jmailagent.entity.InvoiceInfo;
import com.longjunwang.jmailagent.util.OssUtil;
import com.longjunwang.jmailagent.util.TencentUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
public class AttachEventListener implements ApplicationListener<AttachEvent> {

    private final InvoiceService invoiceService;


    @Autowired
    public AttachEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }


    @Override
    public void onApplicationEvent(@NotNull AttachEvent event) {
        log.info(event.toString());
        String url;
        InvoiceInfo invoiceInfo;
        try {
            url = uploadOss(event);
            invoiceInfo = uploadOcr(url, event.getFileName());
            insertInfo(invoiceInfo);
        } catch (Exception e) {
            log.error("attachEvent 发生异常,回滚操作, e: {}", e.getMessage());
            rollBack(event);
        }
    }

    private void rollBack(AttachEvent event) {
        log.info("rollBack: {}", event.getFileName());
        OssUtil.delete(event.getFileName());
    }

    private void insertInfo(InvoiceInfo invoiceInfo) {
        invoiceService.insert(invoiceInfo);
    }

    private InvoiceInfo uploadOcr(String url, String fileName) {
        return TencentUtil.ocr_invoice(new TencentUtil.DTO(null, url, fileName));
    }

    private String uploadOss(AttachEvent event) {
        return Objects.nonNull(event.getFile()) ? OssUtil.uploadByFile(event.getFile()) : OssUtil.uploadByInputStream(event.getInputStream(), event.getFileName());
    }
}
