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
package org.jkiss.dbeaver.model.ai.engine.qwen.dto;// RequestModel.java
import java.util.List;


public class QwenRequest implements BaseRequest{

    public String model;
    public Input input;
    public Parameters parameters;

    public QwenRequest(String model, List<Message> messages, Double temperature, Double topP) {
        this.model = model;
        this.input = new Input(messages);
        this.parameters = new Parameters(temperature, topP);
    }

    // --- 内部类 ---

    public static class Input {
        public List<Message> messages;

        public Input(List<Message> messages) {
            this.messages = messages;
        }
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class Parameters {
        public Double temperature;
        public Double top_p;

        public Parameters(Double temperature, Double top_p) {
            this.temperature = temperature;
            this.top_p = top_p;
        }
    }
}