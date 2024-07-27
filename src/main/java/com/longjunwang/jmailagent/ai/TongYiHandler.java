package com.longjunwang.jmailagent.ai;

import com.longjunwang.jmailagent.tongyi.TongYiApi;
import com.longjunwang.jmailagent.tongyi.TongYiChatModel;
import com.longjunwang.jmailagent.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class TongYiHandler extends AbsAIHandler{

    public TongYiHandler() {
        priority = 1;
        chatModel = new TongYiChatModel(new TongYiApi("https://dashscope.aliyuncs.com/compatible-mode","sk-810ca6e92abd467396f18b84e83067d4"));
        chatClient = ChatClient.create(chatModel);
    }

    @Override
    protected Result callModel(UserMessage userMessage, SystemMessage systemMessage) {
        try {
            return chatClient.prompt()
                    .messages(systemMessage, userMessage)
                    .call()
                    .entity(Result.class);
        } catch (Exception e) {
            log.error("tongyi error", e);
            return null;
        }
    }
}
