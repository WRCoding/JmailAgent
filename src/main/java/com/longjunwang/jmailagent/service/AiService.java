package com.longjunwang.jmailagent.service;

import com.longjunwang.jmailagent.ai.AbsAIHandler;
import com.longjunwang.jmailagent.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class AiService {

    private final List<AbsAIHandler> aiHandlerList;

    @Autowired
    public AiService(List<AbsAIHandler> aiHandlerList) {
        this.aiHandlerList = aiHandlerList;
        //aiHandlerList根据priority进行升序排序
        this.aiHandlerList.sort(Comparator.comparingInt(AbsAIHandler::getPriority));

    }

    public Result call(String userMsg, String sysMsg){
        for (AbsAIHandler aiHandler : aiHandlerList) {
//            if (aiHandler.getClass().getSimpleName().equals("OpenAiHandler")){
//                continue;
//            }
            log.info("aiHandler: {}", aiHandler.getClass().getSimpleName());
            Result result = aiHandler.call(userMsg, sysMsg);
//            log.info("result: {}", result);
            if (Objects.nonNull(result)){
                return result;
            }
        }
        return null;
    }
}
