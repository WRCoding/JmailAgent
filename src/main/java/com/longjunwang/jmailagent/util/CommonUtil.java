package com.longjunwang.jmailagent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longjunwang.jmailagent.entity.Setting;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeUtility;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Objects;

@Slf4j
public class CommonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Setting getSetting(){
        try {
            File file = new File("./setting.json");
            return mapper.readValue(file, Setting.class);
        } catch (IOException e) {
            log.error("getSetting error: {}", e.getMessage());
            return new Setting();
        }
    }

    public static void writeBack(Setting setting){
        try {
            File file = new File("./setting.json");
            mapper.writeValue(file, setting);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date getDateBefore30Days() {
        // 获取当前日期
        LocalDate today = LocalDate.now();
        // 计算前五天的日期
        LocalDate fiveDaysAgo = today.minusDays(30);

        // 将LocalDate转换为Date
        ZonedDateTime zonedDateTime = fiveDaysAgo.atStartOfDay(ZoneId.systemDefault());

        return Date.from(zonedDateTime.toInstant());
    }

    public static Date parse2Date(String date) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            return formatter.parse(date);
        } catch (ParseException e) {
            log.error("date: {} 转换失败", date);
            return getDateBefore30Days();
        }
    }



    public static String decodeText(String text) {
        try {
            return MimeUtility.decodeText(text);
        } catch (Exception e) {
            return text;
        }
    }
}
