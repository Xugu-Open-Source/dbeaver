/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.ai.engine.qwen.dto;// OpenAIChatResponse.java

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class QwenResponseV2 {

    @SerializedName("id")
    private String id;

    @SerializedName("object")
    private String object;

    @SerializedName("created")
    private Long created;

    @SerializedName("model")
    private String model;

    @SerializedName("choices")
    private List<Choice> choices;

    @SerializedName("usage")
    private Usage usage;

    @SerializedName("system_fingerprint")
    private String systemFingerprint;

    // Getters
    public String getId() { return id; }
    public String getObject() { return object; }
    public Long getCreated() { return created; }
    public String getModel() { return model; }
    public List<Choice> getChoices() { return choices; }
    public Usage getUsage() { return usage; }
    public String getSystemFingerprint() { return systemFingerprint; }

    @Override
    public String toString() {
        return "OpenAIChatResponse{" +
                "id='" + id + '\'' +
                ", object='" + object + '\'' +
                ", created=" + created +
                ", model='" + model + '\'' +
                ", choices=" + choices +
                ", usage=" + usage +
                ", systemFingerprint='" + systemFingerprint + '\'' +
                '}';
    }

    // --- 内部类：Choice ---
    public static class Choice {
        @SerializedName("index")
        private Integer index;

        @SerializedName("message")
        private Message message;

        @SerializedName("logprobs")
        private Object logprobs; // 可能是 null 或具体对象

        @SerializedName("finish_reason")
        private String finishReason;

        // Getters
        public Integer getIndex() { return index; }
        public Message getMessage() { return message; }
        public Object getLogprobs() { return logprobs; }
        public String getFinishReason() { return finishReason; }

        @Override
        public String toString() {
            return "Choice{" +
                    "index=" + index +
                    ", message=" + message +
                    ", logprobs=" + logprobs +
                    ", finishReason='" + finishReason + '\'' +
                    '}';
        }
    }

    // --- 内部类：Message ---
    public static class Message {
        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;

        // Getters
        public String getRole() { return role; }
        public String getContent() { return content; }

        @Override
        public String toString() {
            return "Message{" +
                    "role='" + role + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }

    // --- 内部类：Usage ---
    public static class Usage {
        @SerializedName("prompt_tokens")
        private Integer promptTokens;

        @SerializedName("completion_tokens")
        private Integer completionTokens;

        @SerializedName("total_tokens")
        private Integer totalTokens;

        @SerializedName("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;

        // Getters
        public Integer getPromptTokens() { return promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public PromptTokensDetails getPromptTokensDetails() { return promptTokensDetails; }

        @Override
        public String toString() {
            return "Usage{" +
                    "promptTokens=" + promptTokens +
                    ", completionTokens=" + completionTokens +
                    ", totalTokens=" + totalTokens +
                    ", promptTokensDetails=" + promptTokensDetails +
                    '}';
        }
    }

    // --- 内部类：PromptTokensDetails ---
    public static class PromptTokensDetails {
        @SerializedName("cached_tokens")
        private Integer cachedTokens;

        // Getter
        public Integer getCachedTokens() { return cachedTokens; }

        @Override
        public String toString() {
            return "PromptTokensDetails{" +
                    "cachedTokens=" + cachedTokens +
                    '}';
        }
    }
}