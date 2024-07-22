package com.longjunwang.jmailagent.tongyi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.model.ModelDescription;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TongYiApi {


    public static final String DEFAULT_CHAT_MODEL = ChatModel.QWEN_MAX_LONGCONTEXT.getValue();
    private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

    private final RestClient restClient;

    private final WebClient webClient;

    public static Consumer<HttpHeaders> getJsonContentHeaders(String apiKey) {
        return (headers) -> {
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
        };
    };

    public TongYiApi(String baseUrl, String openAiToken) {
        this(baseUrl, openAiToken, RestClient.builder(), WebClient.builder(),RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
    }


    public TongYiApi(String baseUrl, String openAiToken, RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(getJsonContentHeaders(openAiToken))
                .defaultStatusHandler(responseErrorHandler)
                .build();

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(getJsonContentHeaders(openAiToken))
                .build();
    }


    public enum ChatModel implements ModelDescription {
        QWEN_TURBO("qwen-turbo"),
        QWEN_MAX("qwen-max"),
        QWEN_MAX_LONGCONTEXT("qwen-max-longcontext")
        ;
        public final String  value;

        ChatModel(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String getModelName() {
            return this.value;
        }
    }

    /**
     * Represents a tool the model may call. Currently, only functions are supported as a tool.
     *
     * @param type The type of the tool. Currently, only 'function' is supported.
     * @param function The function definition.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FunctionTool(
            @JsonProperty("type") Type type,
            @JsonProperty("function") Function function) {

        /**
         * Create a tool of type 'function' and the given function definition.
         * @param function function definition.
         */
        @ConstructorBinding
        public FunctionTool(Function function) {
            this(Type.FUNCTION, function);
        }

        /**
         * Create a tool of type 'function' and the given function definition.
         */
        public enum Type {
            /**
             * Function tool type.
             */
            @JsonProperty("function") FUNCTION
        }

        /**
         * Function definition.
         *
         * @param description A description of what the function does, used by the model to choose when and how to call
         * the function.
         * @param name The name of the function to be called. Must be a-z, A-Z, 0-9, or contain underscores and dashes,
         * with a maximum length of 64.
         * @param parameters The parameters the functions accepts, described as a JSON Schema object. To describe a
         * function that accepts no parameters, provide the value {"type": "object", "properties": {}}.
         */
        public record Function(
                @JsonProperty("description") String description,
                @JsonProperty("name") String name,
                @JsonProperty("parameters") Map<String, Object> parameters) {

            /**
             * Create tool function definition.
             *
             * @param description tool function description.
             * @param name tool function name.
             * @param jsonSchema tool function schema as json.
             */
            @ConstructorBinding
            public Function(String description, String name, String jsonSchema) {
                this(description, name, ModelOptionsUtils.jsonToMap(jsonSchema));
            }
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionRequest (
            @JsonProperty("messages") List<ChatCompletionMessage> messages,
            @JsonProperty("model") String model,
            @JsonProperty("frequency_penalty") Float frequencyPenalty,
            @JsonProperty("logit_bias") Map<String, Integer> logitBias,
            @JsonProperty("logprobs") Boolean logprobs,
            @JsonProperty("top_logprobs") Integer topLogprobs,
            @JsonProperty("max_tokens") Integer maxTokens,
            @JsonProperty("n") Integer n,
            @JsonProperty("presence_penalty") Float presencePenalty,
            @JsonProperty("response_format") ResponseFormat responseFormat,
            @JsonProperty("seed") Integer seed,
            @JsonProperty("stop") List<String> stop,
            @JsonProperty("stream") Boolean stream,
            @JsonProperty("temperature") Float temperature,
            @JsonProperty("top_p") Float topP,
            @JsonProperty("tools") List<FunctionTool> tools,
            @JsonProperty("tool_choice") Object toolChoice,
            @JsonProperty("user") String user) {

        /**
         * Shortcut constructor for a chat completion request with the given messages and model.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param temperature What sampling temperature to use, between 0 and 1.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Float temperature) {
            this(messages, model, null, null, null, null, null, null, null,
                    null, null, null, false, temperature, null,
                    null, null, null);
        }

        /**
         * Shortcut constructor for a chat completion request with the given messages, model and control for streaming.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param temperature What sampling temperature to use, between 0 and 1.
         * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events
         * as they become available, with the stream terminated by a data: [DONE] message.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model, Float temperature, boolean stream) {
            this(messages, model, null, null, null, null, null, null, null,
                    null, null, null, stream, temperature, null,
                    null, null, null);
        }

        /**
         * Shortcut constructor for a chat completion request with the given messages, model, tools and tool choice.
         * Streaming is set to false, temperature to 0.8 and all other parameters are null.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param model ID of the model to use.
         * @param tools A list of tools the model may call. Currently, only functions are supported as a tool.
         * @param toolChoice Controls which (if any) function is called by the model.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages, String model,
                                     List<FunctionTool> tools, Object toolChoice) {
            this(messages, model, null, null, null, null, null, null, null,
                    null, null, null, false, 0.8f, null,
                    tools, toolChoice, null);
        }

        /**
         * Shortcut constructor for a chat completion request with the given messages, model, tools and tool choice.
         * Streaming is set to false, temperature to 0.8 and all other parameters are null.
         *
         * @param messages A list of messages comprising the conversation so far.
         * @param stream If set, partial message deltas will be sent.Tokens will be sent as data-only server-sent events
         * as they become available, with the stream terminated by a data: [DONE] message.
         */
        public ChatCompletionRequest(List<ChatCompletionMessage> messages,String model, Boolean stream) {
            this(messages, model, null, null, null, null, null, null, null,
                    null, null, null, stream, null, null,
                    null, null, null);
        }

        /**
         * Helper factory that creates a tool_choice of type 'none', 'auto' or selected function by name.
         */
        public static class ToolChoiceBuilder {
            /**
             * Model can pick between generating a message or calling a function.
             */
            public static final String AUTO = "auto";
            /**
             * Model will not call a function and instead generates a message
             */
            public static final String NONE = "none";

            /**
             * Specifying a particular function forces the model to call that function.
             */
            public static Object FUNCTION(String functionName) {
                return Map.of("type", "function", "function", Map.of("name", functionName));
            }
        }

        /**
         * An object specifying the format that the model must output.
         * @param type Must be one of 'text' or 'json_object'.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ResponseFormat(
                @JsonProperty("type") String type) {
        }
    }

    /**
     * Message comprising the conversation.
     *
     * @param rawContent The contents of the message. Can be either a {@link MediaContent} or a {@link String}.
     * The response message content is always a {@link String}.
     * @param role The role of the messages author. Could be one of the {@link Role} types.
     * @param name An optional name for the participant. Provides the model information to differentiate between
     * participants of the same role. In case of Function calling, the name is the function name that the message is
     * responding to.
     * @param toolCallId Tool call that this message is responding to. Only applicable for the {@link Role#TOOL} role
     * and null otherwise.
     * @param toolCalls The tool calls generated by the model, such as function calls. Applicable only for
     * {@link Role#ASSISTANT} role and null otherwise.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionMessage(
            @JsonProperty("content") Object rawContent,
            @JsonProperty("role") Role role,
            @JsonProperty("name") String name,
            @JsonProperty("tool_call_id") String toolCallId,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls) {

        /**
         * Get message content as String.
         */
        public String content() {
            if (this.rawContent == null) {
                return null;
            }
            if (this.rawContent instanceof String text) {
                return text;
            }
            throw new IllegalStateException("The content is not a string!");
        }

        /**
         * Create a chat completion message with the given content and role. All other fields are null.
         * @param content The contents of the message.
         * @param role The role of the author of this message.
         */
        public ChatCompletionMessage(Object content, Role role) {
            this(content, role, null, null, null);
        }

        /**
         * The role of the author of this message.
         */
        public enum Role {
            /**
             * System message.
             */
            @JsonProperty("system") SYSTEM,
            /**
             * User message.
             */
            @JsonProperty("user") USER,
            /**
             * Assistant message.
             */
            @JsonProperty("assistant") ASSISTANT,
            /**
             * Tool message.
             */
            @JsonProperty("tool") TOOL
        }

        /**
         * An array of content parts with a defined type.
         * Each MediaContent can be of either "text" or "image_url" type. Not both.
         *
         * @param type Content  type, each can be of type text or image_url.
         * @param text The text content of the message.
         * @param imageUrl The image content of the message. You can pass multiple
         * images by adding multiple image_url content parts. Image input is only
         * supported when using the gpt-4-visual-preview model.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record MediaContent(
                @JsonProperty("type") String type,
                @JsonProperty("text") String text,
                @JsonProperty("image_url") ImageUrl imageUrl) {

            /**
             * @param url Either a URL of the image or the base64 encoded image data.
             * The base64 encoded image data must have a special prefix in the following format:
             * "data:{mimetype};base64,{base64-encoded-image-data}".
             * @param detail Specifies the detail level of the image.
             */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record ImageUrl(
                    @JsonProperty("url") String url,
                    @JsonProperty("detail") String detail) {

                public ImageUrl(String url) {
                    this(url, null);
                }
            }

            /**
             * Shortcut constructor for a text content.
             * @param text The text content of the message.
             */
            public MediaContent(String text) {
                this("text", text, null);
            }

            /**
             * Shortcut constructor for an image content.
             * @param imageUrl The image content of the message.
             */
            public MediaContent(ImageUrl imageUrl) {
                this("image_url", null, imageUrl);
            }
        }
        /**
         * The relevant tool call.
         *
         * @param id The ID of the tool call. This ID must be referenced when you submit the tool outputs in using the
         * Submit tool outputs to run endpoint.
         * @param type The type of tool call the output is required for. For now, this is always function.
         * @param function The function definition.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ToolCall(
                @JsonProperty("id") String id,
                @JsonProperty("type") String type,
                @JsonProperty("function") ChatCompletionFunction function) {
        }

        /**
         * The function definition.
         *
         * @param name The name of the function.
         * @param arguments The arguments that the model expects you to pass to the function.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ChatCompletionFunction(
                @JsonProperty("name") String name,
                @JsonProperty("arguments") String arguments) {
        }
    }

    public static  String getTextContent(List<ChatCompletionMessage.MediaContent> content) {
        return content.stream()
                .filter(c -> "text".equals(c.type()))
                .map(ChatCompletionMessage.MediaContent::text)
                .reduce("", (a, b) -> a + b);
    }

    /**
     * The reason the model stopped generating tokens.
     */
    public enum ChatCompletionFinishReason {
        /**
         * The model hit a natural stop point or a provided stop sequence.
         */
        @JsonProperty("stop") STOP,
        /**
         * The maximum number of tokens specified in the request was reached.
         */
        @JsonProperty("length") LENGTH,
        /**
         * The content was omitted due to a flag from our content filters.
         */
        @JsonProperty("content_filter") CONTENT_FILTER,
        /**
         * The model called a tool.
         */
        @JsonProperty("tool_calls") TOOL_CALLS,
        /**
         * (deprecated) The model called a function.
         */
        @JsonProperty("function_call") FUNCTION_CALL,
        /**
         * Only for compatibility with Mistral AI API.
         */
        @JsonProperty("tool_call") TOOL_CALL
    }

    /**
     * Represents a chat completion response returned by model, based on the provided input.
     *
     * @param id A unique identifier for the chat completion.
     * @param choices A list of chat completion choices. Can be more than one if n is greater than 1.
     * @param created The Unix timestamp (in seconds) of when the chat completion was created.
     * @param model The model used for the chat completion.
     * @param systemFingerprint This fingerprint represents the backend configuration that the model runs with. Can be
     * used in conjunction with the seed request parameter to understand when backend changes have been made that might
     * impact determinism.
     * @param object The object type, which is always chat.completion.
     * @param usage Usage statistics for the completion request.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletion(
            @JsonProperty("id") String id,
            @JsonProperty("choices") List<Choice> choices,
            @JsonProperty("created") Long created,
            @JsonProperty("model") String model,
            @JsonProperty("system_fingerprint") String systemFingerprint,
            @JsonProperty("object") String object,
            @JsonProperty("usage") Usage usage) {

        /**
         * Chat completion choice.
         *
         * @param finishReason The reason the model stopped generating tokens.
         * @param index The index of the choice in the list of choices.
         * @param message A chat completion message generated by the model.
         * @param logprobs Log probability information for the choice.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Choice(
                @JsonProperty("finish_reason") ChatCompletionFinishReason finishReason,
                @JsonProperty("index") Integer index,
                @JsonProperty("message") ChatCompletionMessage message,
                @JsonProperty("logprobs") LogProbs logprobs) {

        }
    }

    /**
     * Log probability information for the choice.
     *
     * @param content A list of message content tokens with log probability information.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record LogProbs(
            @JsonProperty("content") List<Content> content) {

        /**
         * Message content tokens with log probability information.
         *
         * @param token The token.
         * @param logprob The log probability of the token.
         * @param probBytes A list of integers representing the UTF-8 bytes representation
         * of the token. Useful in instances where characters are represented by multiple
         * tokens and their byte representations must be combined to generate the correct
         * text representation. Can be null if there is no bytes representation for the token.
         * @param topLogprobs List of the most likely tokens and their log probability,
         * at this token position. In rare cases, there may be fewer than the number of
         * requested top_logprobs returned.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record Content(
                @JsonProperty("token") String token,
                @JsonProperty("logprob") Float logprob,
                @JsonProperty("bytes") List<Integer> probBytes,
                @JsonProperty("top_logprobs") List<TopLogProbs> topLogprobs) {

            /**
             * The most likely tokens and their log probability, at this token position.
             *
             * @param token The token.
             * @param logprob The log probability of the token.
             * @param probBytes A list of integers representing the UTF-8 bytes representation
             * of the token. Useful in instances where characters are represented by multiple
             * tokens and their byte representations must be combined to generate the correct
             * text representation. Can be null if there is no bytes representation for the token.
             */
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public record TopLogProbs(
                    @JsonProperty("token") String token,
                    @JsonProperty("logprob") Float logprob,
                    @JsonProperty("bytes") List<Integer> probBytes) {
            }
        }
    }

    /**
     * Usage statistics for the completion request.
     *
     * @param completionTokens Number of tokens in the generated completion. Only applicable for completion requests.
     * @param promptTokens Number of tokens in the prompt.
     * @param totalTokens Total number of tokens used in the request (prompt + completion).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Usage(
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("total_tokens") Integer totalTokens) {

    }

    /**
     * Represents a streamed chunk of a chat completion response returned by model, based on the provided input.
     *
     * @param id A unique identifier for the chat completion. Each chunk has the same ID.
     * @param choices A list of chat completion choices. Can be more than one if n is greater than 1.
     * @param created The Unix timestamp (in seconds) of when the chat completion was created. Each chunk has the same
     * timestamp.
     * @param model The model used for the chat completion.
     * @param systemFingerprint This fingerprint represents the backend configuration that the model runs with. Can be
     * used in conjunction with the seed request parameter to understand when backend changes have been made that might
     * impact determinism.
     * @param object The object type, which is always 'chat.completion.chunk'.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatCompletionChunk(
            @JsonProperty("id") String id,
            @JsonProperty("choices") List<ChunkChoice> choices,
            @JsonProperty("created") Long created,
            @JsonProperty("model") String model,
            @JsonProperty("system_fingerprint") String systemFingerprint,
            @JsonProperty("object") String object) {

        /**
         * Chat completion choice.
         *
         * @param finishReason The reason the model stopped generating tokens.
         * @param index The index of the choice in the list of choices.
         * @param delta A chat completion delta generated by streamed model responses.
         * @param logprobs Log probability information for the choice.
         */
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public record ChunkChoice(
                @JsonProperty("finish_reason") ChatCompletionFinishReason finishReason,
                @JsonProperty("index") Integer index,
                @JsonProperty("delta") ChatCompletionMessage delta,
                @JsonProperty("logprobs") LogProbs logprobs) {
        }
    }


    public ResponseEntity<ChatCompletion> chatCompletionEntity(ChatCompletionRequest chatRequest) {

        Assert.notNull(chatRequest, "The request body can not be null.");
        Assert.isTrue(!chatRequest.stream(), "Request must set the steam property to false.");

        return this.restClient.post()
                .uri("/v1/chat/completions")
                .body(chatRequest)
                .retrieve()
                .toEntity(ChatCompletion.class);
    }
}
