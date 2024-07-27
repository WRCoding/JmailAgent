package com.longjunwang.jmailagent.service;

import com.longjunwang.jmailagent.browser.BrowserService;
import com.longjunwang.jmailagent.entity.FailUrl;
import com.longjunwang.jmailagent.entity.Setting;
import com.longjunwang.jmailagent.mapper.FailUrlMapper;
import com.longjunwang.jmailagent.util.CommonPrompt;
import com.longjunwang.jmailagent.util.CommonUtil;
import com.longjunwang.jmailagent.util.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class MailService {

    @Value("${JmailAgent.attachment.location}")
    private String location;

    @Value("${JmailAgent.attachment.suffix}")
    private String suffix;

    @Value("${JmailAgent.mail.username}")
    private String mailUser;

    @Value("${JmailAgent.mail.password}")
    private String mailPassword;

    @Resource
    private AiService aiService;
    @Resource
    private BrowserService browserService;

    @Resource
    private FailUrlMapper failUrlMapper;
    private Store store;
    private Folder inbox;


    private final List<String> urls = new ArrayList<>();

    private volatile boolean isInvoking = false;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 用单线程池控制串行跑
     */
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MICROSECONDS, new LinkedBlockingDeque<>(3));


    @PostConstruct
    public void init() {
        connectFolder();
    }

    private void connectFolder() {
        try {
            Properties properties = new Properties();
            properties.setProperty("mail.store.protocol", "imap");
            properties.setProperty("mail.imap.ssl.enable", "true");
            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUser, mailPassword);
                }
            });
            store = session.getStore("imap");
            store.connect("imap.qq.com", 993, mailUser, mailPassword);
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            cleanFolder(location);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查链接和重新链接
     */
    public void checkConnectAndReconnect(){
        if (Objects.nonNull(store) && store.isConnected()){
            log.info("链接正常,无须重连");
            return;
        }
        log.info("重新链接IMAP....");
        reconnectFolder();
    }

    public void reconnectFolder() {
        if (!isInvoking) {
            synchronized (this){
                if (!isInvoking){
                    shutdownFolder();
                    connectFolder();
                }
            }
        } else {
            log.error("正在操作邮箱,禁止重连");
        }
    }

    public void cleanFolder(String path) {
        File folder = new File(path);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // 递归删除子目录
                        cleanFolder(file.getPath());
                    } else {
                        // 删除文件
                        file.delete();
                    }
                }
            }
        } else {
            folder.mkdirs();
        }
    }

    public void invoke() {
        if (!isInvoking) {
            lock.lock();
            try {
                if (!isInvoking) {
                    try {
                        isInvoking = true;
                        log.info("正在执行任务.....");
                        checkConnectAndReconnect();
                        searchAndParseMail();
                        saveFailUrl();
                    } catch (Exception e) {
                        log.error("任务执行失败, e: {}", e.getMessage());
                    } finally {
                        isInvoking = false;
                    }
                }
            } finally {
                lock.unlock();
            }
        }else{
            log.info("任务正在执行,请勿重复执行");
        }
    }

    private void saveFailUrl() {
        for (String url : BrowserService.failedUrl) {
            failUrlMapper.insert(new FailUrl(url));
        }
    }

    public void searchAndParseMail() throws MessagingException, InterruptedException {
        Setting setting = CommonUtil.getSetting();
        log.info("执行开始: setting: {}", setting);
        long start = System.currentTimeMillis();
        String since = setting.getSince();
        String subject = setting.getSubject();
        Integer lastEmailId = setting.getLastEmailId();
        SearchTerm searchTerm = initSearchTerm(since);
        Message[] messages = filterMessagesBySubject(inbox.search(searchTerm), subject);
        log.info("message size: {}", messages.length);
        int count = 0;
        for (Message message : messages) {
            if (message.getMessageNumber() <= lastEmailId) {
                continue;
            }
            count++;
            lastEmailId = Math.max(lastEmailId, message.getMessageNumber());
            metaData(message);
            BodyPart attachment = getAttachment(message);
            if (Objects.nonNull(attachment)) {
                parseAttachment(attachment);
            } else {
                TimeUnit.SECONDS.sleep(1);
                Result result = aiService.call(extractHtmlContent(message), CommonPrompt.HTML_PROMPT);
                urls.add(result.getResult());
                log.info("url: {}", result.getResult());
            }
        }
//        if (count > 0) {
//            log.info("开始处理外部, url size: {}", urls.size());
//            browserService.parse_url(urls);
//            setting.setLastEmailId(lastEmailId);
//            CommonUtil.writeBack(setting);
//        }
//        cleanFolder(location);
        log.info("执行完成, 处理数: {}, 耗时: {}", count, (System.currentTimeMillis() - start) / 1000);

    }

    private void metaData(Message message) throws MessagingException {
        SimpleDateFormat formatter = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
        String receiveDate = formatter.format(message.getReceivedDate());
        String subject = message.getSubject();
        InternetAddress address = (InternetAddress) message.getFrom()[message.getFrom().length - 1];
        log.info("MailId: {}, From: {}, Subject: {}, receiveDate: {}", message.getMessageNumber(), address.getAddress(), subject, receiveDate);
    }

    public void parseHtml(Message message) {
        try {
            String fileName = message.getSubject() + ".html";
            String htmlContent = extractHtmlContent(message);
            if (htmlContent != null) {
                File file = new File(location + File.separator + fileName);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(htmlContent);
                }
            }
        } catch (Exception e) {
            log.error("获取HTML异常: e: {}", e.getMessage());
        }
    }

    private String extractHtmlContent(Part part) {
        if (Objects.isNull(part)) {
            return null;
        }
        try {
            if (part.isMimeType("text/html")) {
                return (String) part.getContent();
            } else if (part.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) part.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    String htmlContent = extractHtmlContent(multipart.getBodyPart(i));
                    if (htmlContent != null) {
                        return htmlContent;
                    }
                }
            }
        } catch (Exception e) {
            log.error("extractHtmlContent error: {}", e.getMessage());
        }
        return null;
    }

    public BodyPart getAttachment(Message message) {
        try {
            if (message.isMimeType("multipart/*")) {
                Multipart multipart = (Multipart) message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && suffix.contains(getFileSuffix(CommonUtil.decodeText(bodyPart.getFileName())))) {
                        return bodyPart;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("获取附件异常: e: {}", e.getMessage());
            return null;
        }
    }

    private String getFileSuffix(String fileName) {
        int index = fileName.lastIndexOf(".") + 1;
        return fileName.substring(index);
    }

    private void parseAttachment(BodyPart bodyPart) {
        try {
            InputStream is = bodyPart.getInputStream();
            File file = new File(location + File.separator + CommonUtil.decodeText(bodyPart.getFileName()));
            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buf = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buf)) != -1) {
                    fos.write(buf, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            log.error("附件保存失败, e: {}", e.getMessage());
        }
    }

    private SearchTerm initSearchTerm(String since) {
        Date date = CommonUtil.getDateBefore30Days();
        if (StringUtils.hasText(since)) {
            date = CommonUtil.parse2Date(since);
        }
        return new ReceivedDateTerm(ComparisonTerm.GE, date);
    }

    private Message[] filterMessagesBySubject(Message[] messages, String subject) {
        return Arrays.stream(messages)
                .filter(message -> {
                    try {
                        String decodedSubject = decodeSubject(message.getSubject());
                        return decodedSubject != null && decodedSubject.contains(subject);
                    } catch (MessagingException | UnsupportedEncodingException e) {
                        return false;
                    }
                })
                .toArray(Message[]::new);
    }

    private String decodeSubject(String subject) throws UnsupportedEncodingException {
        if (subject == null) {
            return null;
        }
        // 使用MimeUtility解码主题
        return MimeUtility.decodeText(subject);
    }

    @PreDestroy
    public void destroy() {
        shutdownFolder();
    }

    private void shutdownFolder() {
        try {
            if (Objects.nonNull(inbox)) {
                inbox.close();
            }
            if (Objects.nonNull(store)) {
                store.close();
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
