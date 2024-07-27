package com.longjunwang.jmailagent.ai;

import com.longjunwang.jmailagent.util.Config;
import com.longjunwang.jmailagent.util.Result;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public abstract class AbsAIHandler {


    protected ChatClient chatClient;
    protected ChatModel chatModel;
    @Getter
    protected int priority;
    public Result call(String userMsg, String SysMsg){
        SystemMessage systemMessage = new SystemMessage(SysMsg);
        UserMessage userMessage = new UserMessage(userMsg);
        return callModel(userMessage, systemMessage);
    }

    protected abstract Result callModel(UserMessage userMessage, SystemMessage systemMessage);

}
