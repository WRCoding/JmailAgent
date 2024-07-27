package com.longjunwang.jmailagent.ai;

import cn.hutool.extra.spring.SpringUtil;
import com.longjunwang.jmailagent.tongyi.TongYiApi;
import com.longjunwang.jmailagent.tongyi.TongYiChatModel;
import com.longjunwang.jmailagent.util.CommonPrompt;
import com.longjunwang.jmailagent.util.Config;
import com.longjunwang.jmailagent.util.Result;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AiService {

    private OpenAiChatModel chatModel;
    private ChatClient chatClient;

    private TongYiChatModel tongYiChatModel;

    @PostConstruct
    public void init(){
        Config config = SpringUtil.getBean(Config.class);
//        chatModel = new OpenAiChatModel(new OpenAiApi(config.getOpenaiBaseUrl(), config.getOpenaiKey()));
        tongYiChatModel = new TongYiChatModel(new TongYiApi("https://dashscope.aliyuncs.com/compatible-mode","sk-810ca6e92abd467396f18b84e83067d4"));
//        chatClient = ChatClient.create(chatModel);
        chatClient = ChatClient.create(tongYiChatModel);
    }

    public Result aiParseHtml(String content, String prompt){
        try {
            SystemMessage systemMessage = new SystemMessage(prompt);
            UserMessage userMessage = new UserMessage(content);
            return call(systemMessage, userMessage);
        }catch (Exception e){
            log.error(e.getMessage());
            return new Result();
        }
    }


    private Result call(SystemMessage systemMessage, UserMessage userMessage) throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        return chatClient.prompt()
//                .options(OpenAiChatOptions.builder().withModel(OpenAiApi.ChatModel.GPT_4_O.getValue()).build())
                .messages(systemMessage, userMessage)
                .call()
                .entity(Result.class);
    }
}
