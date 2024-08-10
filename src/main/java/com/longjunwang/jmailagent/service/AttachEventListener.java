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
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
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
        InvoiceInfo invoiceInfo = null;
        try {
            String url = uploadOss(event);
            invoiceInfo = uploadOcr(event, url);
            handleInvoice(invoiceInfo);
        } catch (Exception e) {
            log.error("attachEvent 发生异常,回滚操作, e: {}", e.getMessage());
            rollBack(invoiceInfo, event);
        }
    }

    private void handleInvoice(InvoiceInfo invoiceInfo) {
        if (invoiceService.containNumber(invoiceInfo.getNumber())){
            OssUtil.delete(invoiceInfo.getFileName());
        }else{
            insertInfo(invoiceInfo);
        }
    }

    private void rollBack(InvoiceInfo invoiceInfo, AttachEvent event) {
        if (Objects.nonNull(invoiceInfo)) {
            log.info("rollBack: {}", invoiceInfo);
            invoiceService.hardDelete(invoiceInfo.getNumber());
        }else {
            OssUtil.delete(event.getFileName());
        }
    }

    private void insertInfo(InvoiceInfo invoiceInfo) {
        invoiceService.insert(invoiceInfo);
    }

    private InvoiceInfo uploadOcr(AttachEvent event, String url) throws Exception {
        return TencentUtil.ocr_invoice(new TencentUtil.DTO(event.getFile(), url, event.getFileName()));
    }

    private String uploadOss(AttachEvent event) {
        return Objects.nonNull(event.getFile()) ? OssUtil.uploadByFile(event) : OssUtil.uploadByInputStream(event);
    }
}
