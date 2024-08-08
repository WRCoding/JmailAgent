package com.longjunwang.jmailagent.util;

import cn.hutool.extra.spring.SpringUtil;
import com.aliyun.oss.*;
import com.aliyun.oss.model.*;
import com.longjunwang.jmailagent.browser.BrowserService;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class OssUtil {
    private static String bucketName = "jmail";

    private static String internalUrl = "https://oss-cn-shenzhen-internal.aliyuncs.com";
    private static String externalUrl = "https://oss-cn-shenzhen.aliyuncs.com";

    public static void uploadFolder(String folder) {
        File file = new File(folder);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            assert files != null;
            Arrays.stream(files).forEach(OssUtil::uploadByFile);
        }
    }

    public static void upload() {
        List<String> filePaths = BrowserService.filePaths;
        for (String filePath : filePaths) {
            File file = new File(filePath);
            OssUtil.uploadByFile(file);
        }
    }

    public static String uploadByFile(File file) {
        return baseUpload(new PutObjectRequest(bucketName, file.getName(), file), file.getName());
    }

    public static String uploadByInputStream(InputStream inputStream, String fileName) {
        return baseUpload(new PutObjectRequest(bucketName, fileName, inputStream), fileName);
    }

    public static String baseUpload(PutObjectRequest putObjectRequest, String objectName) {
        OSS ossClient = getInternalOssClient();
        ossClient.putObject(putObjectRequest);
        ossClient.shutdown();
        return generateTempUrl(objectName);
    }

    private static OSS getInternalOssClient() {
        return getOssClient(internalUrl);
    }

    private static OSS getExternalOssClient() {
        return getOssClient(externalUrl);
    }

    private static OSS getOssClient(String externalUrl) {
        Config config = SpringUtil.getBean(Config.class);
        return new OSSClientBuilder().build(externalUrl, config.getCredentialsProvider());
    }

    public static String generateTempUrl(String objectName) {
        OSS ossClient = getExternalOssClient();
        String tempUrl = null;
        try {
            URL signedUrl;
            // 指定生成的签名URL过期时间，单位为毫秒。本示例以设置过期时间为1小时为例。
            Date expiration = new Date(new Date().getTime() + 3600 * 1000L);

            // 生成签名URL。
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName, HttpMethod.GET);
            // 设置过期时间。
            request.setExpiration(expiration);

            // 通过HTTP GET请求生成签名URL。
            signedUrl = ossClient.generatePresignedUrl(request);
            tempUrl = transferUrl(signedUrl);
            // 打印签名URL。
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        }
        return tempUrl;
    }

    private static String transferUrl(URL signedUrl) {
        String urlString = signedUrl.toString();
        return urlString.replace(internalUrl, externalUrl);
    }

    public static List<String> listObject() {
        OSS ossClient = getInternalOssClient();
        try {
            // 列举文件。如果不设置keyPrefix，则列举存储空间下的所有文件。如果设置keyPrefix，则列举包含指定前缀的文件。
            ObjectListing objectListing = ossClient.listObjects(bucketName);
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
            return sums.stream().map(OSSObjectSummary::getKey).collect(Collectors.toList());
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return new ArrayList<>();
    }

    public static void delete(String objectName) {
        OSS ossClient = getInternalOssClient();
        try {
            // 删除文件或目录。如果要删除目录，目录必须为空。
            ossClient.deleteObject(bucketName, objectName);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}
