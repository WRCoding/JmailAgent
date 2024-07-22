package com.longjunwang.jmailagent.tongyi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.function.AbstractFunctionCallSupport;
import org.springframework.ai.model.function.FunctionCallbackContext;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.metadata.OpenAiChatResponseMetadata;
import org.springframework.ai.openai.metadata.support.OpenAiResponseHeaderExtractor;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.*;

@Slf4j
public class TongYiChatModel extends
        AbstractFunctionCallSupport<TongYiApi.ChatCompletionMessage, TongYiApi.ChatCompletionRequest, ResponseEntity<TongYiApi.ChatCompletion>>
        implements ChatModel, StreamingChatModel {


    private final RetryTemplate retryTemplate;

    private final TongYiApi tongYiApi;

    public TongYiChatModel(TongYiApi tongYiApi) {
        this(tongYiApi, RetryUtils.DEFAULT_RETRY_TEMPLATE, null);
    }

    protected TongYiChatModel(TongYiApi tongYiApi, RetryTemplate retryTemplate, FunctionCallbackContext functionCallbackContext) {
        super(functionCallbackContext);
        this.tongYiApi = tongYiApi;
        this.retryTemplate = retryTemplate;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        TongYiApi.ChatCompletionRequest request = createRequest(prompt, false);

        return this.retryTemplate.execute(ctx -> {

            ResponseEntity<TongYiApi.ChatCompletion> completionEntity = this.callWithFunctionSupport(request);

            var chatCompletion = completionEntity.getBody();
            if (chatCompletion == null) {
                log.warn("No chat completion returned for prompt: {}", prompt);
                return new ChatResponse(List.of());
            }

            RateLimit rateLimits = OpenAiResponseHeaderExtractor.extractAiResponseHeaders(completionEntity);

            List<TongYiApi.ChatCompletion.Choice> choices = chatCompletion.choices();
            if (choices == null) {
                log.warn("No choices returned for prompt: {}", prompt);
                return new ChatResponse(List.of());
            }

            List<Generation> generations = choices.stream().map(choice -> new Generation(choice.message().content(), toMap(chatCompletion.id(), choice))
                    .withGenerationMetadata(ChatGenerationMetadata.from(choice.finishReason().name(), null))).toList();

            return new ChatResponse(generations, null);
        });
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return null;
    }

    TongYiApi.ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {


        List<TongYiApi.ChatCompletionMessage> chatCompletionMessages = prompt.getInstructions().stream().map(m -> {
            // Add text content.
            List<TongYiApi.ChatCompletionMessage.MediaContent> contents = new ArrayList<>(List.of(new TongYiApi.ChatCompletionMessage.MediaContent(m.getContent())));

            return new TongYiApi.ChatCompletionMessage(contents, TongYiApi.ChatCompletionMessage.Role.valueOf(m.getMessageType().name()));
        }).toList();

        return new TongYiApi.ChatCompletionRequest(chatCompletionMessages, TongYiApi.DEFAULT_CHAT_MODEL, stream);
    }

    private Map<String, Object> toMap(String id, TongYiApi.ChatCompletion.Choice choice) {
        Map<String, Object> map = new HashMap<>();

        var message = choice.message();
        if (message.role() != null) {
            map.put("role", message.role().name());
        }
        if (choice.finishReason() != null) {
            map.put("finishReason", choice.finishReason().name());
        }
        map.put("id", id);
        return map;
    }

    @Override
    protected TongYiApi.ChatCompletionRequest doCreateToolResponseRequest(TongYiApi.ChatCompletionRequest previousRequest, TongYiApi.ChatCompletionMessage responseMessage, List<TongYiApi.ChatCompletionMessage> conversationHistory) {
        return null;
    }

    @Override
    protected List<TongYiApi.ChatCompletionMessage> doGetUserMessages(TongYiApi.ChatCompletionRequest request) {
        return request.messages();
    }

    @Override
    protected TongYiApi.ChatCompletionMessage doGetToolResponseMessage(ResponseEntity<TongYiApi.ChatCompletion> response) {
        return null;
    }

    @Override
    protected ResponseEntity<TongYiApi.ChatCompletion> doChatCompletion(TongYiApi.ChatCompletionRequest request) {
        return this.tongYiApi.chatCompletionEntity(request);
    }

    @Override
    protected Flux<ResponseEntity<TongYiApi.ChatCompletion>> doChatCompletionStream(TongYiApi.ChatCompletionRequest request) {
        return null;
    }

    @Override
    protected boolean isToolFunctionCall(ResponseEntity<TongYiApi.ChatCompletion> response) {
        return false;
    }
}
