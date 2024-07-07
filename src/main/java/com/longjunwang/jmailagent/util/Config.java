package com.longjunwang.jmailagent.util;

import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.tencentcloudapi.common.Credential;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class Config {
    @Value("${JmailAgent.oss.keyId}")
    private String ossKeyId;

    @Value("${JmailAgent.oss.secretKey}")
    private String ossSecret;

    @Value("${JmailAgent.ocr.secretId}")
    private String ocrKeyId;

    @Value("${JmailAgent.ocr.secretKey}")
    private String ocrSecret;

    @Value("${spring.ai.openai.api-key}")
    private String openaiKey;

    @Value("${spring.ai.openai.base-url}")
    private String openaiBaseUrl;

    private CredentialsProvider credentialsProvider;

    private Credential credential;


    @PostConstruct
    public void init(){
        credentialsProvider = new DefaultCredentialProvider(ossKeyId, ossSecret);
        credential = new Credential(ocrKeyId, ocrSecret);
    }


}
