package com.longjunwang.jmailagent.service;

import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Service
@Slf4j
public class WebSocketService extends TextWebSocketHandler {


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String msg = message.getPayload();
        log.info("收到消息：{}", msg);
        MailService mailService = SpringUtil.getBean(MailService.class);
        mailService.invoke();
        session.sendMessage(new TextMessage("done"));
    }
}
