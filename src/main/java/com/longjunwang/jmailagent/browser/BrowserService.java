package com.longjunwang.jmailagent.browser;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.extra.spring.SpringUtil;
import com.longjunwang.jmailagent.service.AiService;
import com.longjunwang.jmailagent.util.CommonPrompt;
import com.longjunwang.jmailagent.util.AttachFileWatcher;
import com.longjunwang.jmailagent.util.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service()
@Lazy()
@Data
public class BrowserService {

    @Resource
    private AiService aiService;

    @Value("${JmailAgent.attachment.location}")
    private String location;

    private ChromeDriver chromeDriver;
    private WebDriverWait driverWait;
    private String originHandle;

    public static List<String> filePaths = new ArrayList<>();

    public static List<String> failedUrl = new ArrayList<>();

    private void initChrome() {
        if (Objects.nonNull(chromeDriver)){
            chromeDriver.close();
        }
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", location); // 替换为你的下载路径
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", prefs);
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        chromeDriver = new ChromeDriver(options);
        driverWait = new WebDriverWait(chromeDriver, Duration.ofSeconds(10));
        WatchMonitor watchMonitor = WatchMonitor.create(new File(location), WatchMonitor.ENTRY_CREATE);
        watchMonitor.setWatcher(SpringUtil.getBean(AttachFileWatcher.class));
        watchMonitor.start();
        originHandle = chromeDriver.getWindowHandle();
        log.info("origin handle: {}", originHandle);
    }

    public void parse_url(List<String> urls) {
        initChrome();
        List<List<String>> splitList = splitList(urls,4);
        try {
            for (List<String> urlList : splitList) {
                log.info("urlList: {}", urlList);
                for (Map.Entry<String, String> entry : openWindows(urlList).entrySet()) {
                    String url = entry.getKey();
                    String handle = entry.getValue();
                    chromeDriver.switchTo().window(handle);
                    driverWait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                    String htmlContent = chromeDriver.getPageSource();
                    Result result = aiService.call(htmlContent, CommonPrompt.PDF_PROMPT);
                    log.info("url: {}, parse result: {}", url, result);
                    if (Objects.nonNull(result) && StringUtils.hasText(result.getResult())) {
                        try {
                            String text = result.getResult();
                            String xpathExpression = String.format("//*[contains(text(), '%s')]", text);
                            WebElement btn = driverWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathExpression)));
                            btn.click();
                        } catch (Exception e) {
                            log.info("click url: {} error: {}", url, e.getMessage());
                            failedUrl.add(url);
                        }
                    } else {
                        log.info("url: {}, parse null", url);
                        failedUrl.add(url);
                    }
                }
            }
        } catch (Exception e) {
            log.error("parse_url error e: {}", e.getMessage());
        }

    }

    public Map<String, String> openWindows(List<String> urls) {
        chromeDriver.switchTo().window(originHandle);
        Map<String, String> urlHandleMap = new HashMap<>();
        for (String url : urls) {
            try {
                Set<String> currentHandles = chromeDriver.getWindowHandles();
                chromeDriver.executeScript(String.format("window.open('%s')", url));
                Set<String> newHandles = chromeDriver.getWindowHandles();
                newHandles.removeAll(currentHandles);
                urlHandleMap.put(url, newHandles.toArray(new String[]{})[0]);
            } catch (Exception e) {
                log.error("open url: {} error: {}", url, e.getMessage());
            }
        }
        return urlHandleMap;
    }


    public static <T> List<List<T>> splitList(List<T> originalList, int chunkSize) {
        List<List<T>> splitList = new ArrayList<>();
        int listSize = originalList.size();

        for (int i = 0; i < listSize; i += chunkSize) {
            splitList.add(new ArrayList<>(
                    originalList.subList(i, Math.min(listSize, i + chunkSize))
            ));
        }

        return splitList;
    }
}
