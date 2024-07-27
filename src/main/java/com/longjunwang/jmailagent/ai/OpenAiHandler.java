package com.longjunwang.jmailagent.ai;

import com.longjunwang.jmailagent.util.Config;
import com.longjunwang.jmailagent.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class OpenAiHandler extends AbsAIHandler{



    @Autowired
    public OpenAiHandler(Config config) {
        priority = 0;
        chatModel = new OpenAiChatModel(new OpenAiApi(config.getOpenaiBaseUrl(), config.getOpenaiKey()));
        chatClient = ChatClient.create(chatModel);
    }

    @Override
    protected Result callModel(UserMessage userMessage, SystemMessage systemMessage) {
        try {
            return chatClient.prompt()
                    .options(OpenAiChatOptions.builder().withModel(OpenAiApi.ChatModel.GPT_3_5_TURBO.getValue()).build())
                    .messages(systemMessage, userMessage)
                    .call()
                    .entity(Result.class);
        } catch (Exception e) {
            log.error("openai error", e);
            return null;
        }
    }
}
