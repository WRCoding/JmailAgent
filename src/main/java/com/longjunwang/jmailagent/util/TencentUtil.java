package com.longjunwang.jmailagent.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.longjunwang.jmailagent.entity.InvoiceInfo;
import com.longjunwang.jmailagent.service.InvoiceService;
import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class TencentUtil {


    public static InvoiceInfo ocr_invoice(File file) {
        try {
            OcrClient client = getOcrClient();
            // 实例化一个请求对象,每个接口都会对应一个request对象
            RecognizeGeneralInvoiceRequest req = new RecognizeGeneralInvoiceRequest();
            req.setImageBase64(transfer2Base64(file));
            req.setEnablePdf(true);
            // 返回的resp是一个RecognizeGeneralInvoiceResponse的实例，与请求对象对应
            RecognizeGeneralInvoiceResponse resp = client.RecognizeGeneralInvoice(req);
            return extractData(resp,file);
        } catch (Exception e) {
            log.info("ocr_invoice path: {} error: {}", file.getPath(), e.getMessage());
            return null;
        }
    }

    @NotNull
    private static OcrClient getOcrClient() {
        Config config = SpringUtil.getBean(Config.class);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("ocr.tencentcloudapi.com");
        // 实例化一个client选项，可选的，没有特殊需求可以跳过
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        // 实例化要请求产品的client对象,clientProfile是可选的
        return new OcrClient(config.getCredential(), "ap-guangzhou", clientProfile);
    }

    private static InvoiceInfo extractData(RecognizeGeneralInvoiceResponse resp, File file) throws NoSuchFieldException, IllegalAccessException {
        String response = AbstractModel.toJsonString(resp);
        RecognizeGeneralInvoiceResponse generalInvoiceResponse = AbstractModel.fromJsonString(response, RecognizeGeneralInvoiceResponse.class);
        Object objectInvoiceInfo = getObjectInvoiceInfo(generalInvoiceResponse);
        if (objectInvoiceInfo instanceof VatInvoiceInfo vatInvoiceInfo){
            InvoiceInfo invoiceInfo = new InvoiceInfo();
            BeanUtil.copyProperties(vatInvoiceInfo, invoiceInfo);
            invoiceInfo.setName(vatInvoiceInfo.getVatInvoiceItemInfos()[0].getName());
            invoiceInfo.setFileName(file.getName());
            return invoiceInfo;
        }else if (objectInvoiceInfo instanceof VatElectronicInfo vatElectronicInfo) {
            InvoiceInfo invoiceInfo = new InvoiceInfo();
            BeanUtil.copyProperties(vatElectronicInfo, invoiceInfo);
            invoiceInfo.setName(vatElectronicInfo.getVatElectronicItems()[0].getName());
            invoiceInfo.setFileName(file.getName());
            return invoiceInfo;
        }else{
            return null;
        }
    }

    private static Object getObjectInvoiceInfo(RecognizeGeneralInvoiceResponse generalInvoiceResponse) throws NoSuchFieldException, IllegalAccessException {
        InvoiceItem mixedInvoiceItem = generalInvoiceResponse.getMixedInvoiceItems()[0];
        String subType = mixedInvoiceItem.getSubType();
        SingleInvoiceItem singleInvoiceInfos = mixedInvoiceItem.getSingleInvoiceInfos();
        Class<? extends SingleInvoiceItem> clazz = singleInvoiceInfos.getClass();
        Field clazzDeclaredField = clazz.getDeclaredField(subType);
        clazzDeclaredField.setAccessible(true);
        return clazzDeclaredField.get(singleInvoiceInfos);
    }

    private static String transfer2Base64(File file) throws IOException {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
    }

}
