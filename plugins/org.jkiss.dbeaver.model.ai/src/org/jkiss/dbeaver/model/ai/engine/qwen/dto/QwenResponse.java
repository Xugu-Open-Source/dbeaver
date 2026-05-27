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
package org.jkiss.dbeaver.model.ai.engine.qwen.dto;// QwenResponse.java
import com.google.gson.annotations.SerializedName;


public class QwenResponse {

    @SerializedName("output")
    private Output output;

    @SerializedName("usage")
    private Usage usage;

    @SerializedName("request_id")
    private String requestId;

    // Getters
    public Output getOutput() {
        return output;
    }

    public Usage getUsage() {
        return usage;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        return "QwenResponse{" +
                "output=" + output +
                ", usage=" + usage +
                ", requestId='" + requestId + '\'' +
                '}';
    }

    // --- 内部类：output ---
    public static class Output {
        @SerializedName("finish_reason")
        private String finishReason;

        @SerializedName("text")
        private String text;

        // Getters
        public String getFinishReason() {
            return finishReason;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return "Output{" +
                    "finishReason='" + finishReason + '\'' +
                    ", text='" + text + '\'' +
                    '}';
        }
    }

    // --- 内部类：usage ---
    public static class Usage {
        @SerializedName("input_tokens")
        private Integer inputTokens;

        @SerializedName("output_tokens")
        private Integer outputTokens;

        @SerializedName("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;

        @SerializedName("total_tokens")
        private Integer totalTokens;

        // Getters
        public Integer getInputTokens() {
            return inputTokens;
        }

        public Integer getOutputTokens() {
            return outputTokens;
        }

        public PromptTokensDetails getPromptTokensDetails() {
            return promptTokensDetails;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        @Override
        public String toString() {
            return "Usage{" +
                    "inputTokens=" + inputTokens +
                    ", outputTokens=" + outputTokens +
                    ", promptTokensDetails=" + promptTokensDetails +
                    ", totalTokens=" + totalTokens +
                    '}';
        }
    }

    // --- 内部类：prompt_tokens_details ---
    public static class PromptTokensDetails {
        @SerializedName("cached_tokens")
        private Integer cachedTokens;

        // Getter
        public Integer getCachedTokens() {
            return cachedTokens;
        }

        @Override
        public String toString() {
            return "PromptTokensDetails{" +
                    "cachedTokens=" + cachedTokens +
                    '}';
        }
    }
}