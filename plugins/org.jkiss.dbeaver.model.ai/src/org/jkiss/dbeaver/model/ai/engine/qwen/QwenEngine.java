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

// QwenEngine.java
package org.jkiss.dbeaver.model.ai.engine.qwen;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.ai.engine.*;
import org.jkiss.dbeaver.model.ai.engine.qwen.dto.*;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.ai.utils.AIHttpUtils;
import org.jkiss.dbeaver.model.ai.utils.DisposableLazyValue;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class QwenEngine extends BaseCompletionEngine<QwenProperties> {
    protected static final Duration TIMEOUT = Duration.ofSeconds(30);

    protected static final Gson GSON = JSONUtils.GSON;

    private final DisposableLazyValue<QwenClient, DBException> client = new DisposableLazyValue<>() {
        @Override
        protected void onDispose(QwenClient disposedValue) throws DBException {
            disposedValue.close();
        }

        @NotNull
        @Override
        protected QwenClient initialize() {
            return createClient();
        }
    };


    public QwenEngine(@NotNull QwenProperties properties) {
        super(properties);
    }

    @NotNull
    protected QwenClient createClient()   {
        String token = properties.getToken();
        String baseUrl = properties.getBaseUrl();
        if (baseUrl == null || baseUrl.isEmpty()) {
            baseUrl = QwenConstants.DEFAULT_BASE_URL;
        }
        return QwenClient.createClient(baseUrl, token);
    }

    @Override
    public List<AIModel> getModels(DBRProgressMonitor monitor) throws DBException {
       return QwenModels.KNOWN_MODELS.values().stream().toList();
    }

    @Override
    public AIEngineResponse requestCompletion(DBRProgressMonitor monitor, AIEngineRequest request) throws DBException {
        // 构建请求
        HttpRequest httpRequest = buildRequest(request);
        String send = client.getInstance().client.send(monitor, httpRequest);
//        HttpResponse<String> response = client.getInstance().client.send(monitor,httpRequest);
        // 解析消息体
        return paseRValue(send);
    }

    public AIEngineResponse paseRValue(String body/*, HttpResponse<String> response*/)throws DBException{
//        if (response.statusCode() == 200) {
            QwenResponse qwenResponse = GSON.fromJson(body, QwenResponse.class);
            String text = null;
            try {
                text = qwenResponse.getOutput().getText();
            } catch (Exception e) {
                try {
                    QwenResponseV2 qwenResponseV2 = GSON.fromJson(body, QwenResponseV2.class);
                    text = qwenResponseV2.getChoices().get(0).getMessage().getContent();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (text.isEmpty()){
                return new AIEngineResponse(AIMessageType.ASSISTANT, List.of(AIMessages.ai_empty_engine_response));
            }
            ArrayList<String> strings = new ArrayList<>();
            strings.add(text);
            return new AIEngineResponse(AIMessageType.ASSISTANT, strings);
//        } else if (response.statusCode() == 429) {
//            throw new TooManyRequestsException("Too many requests: " + body);
//        } else {
//            throw new DBException("Qwen request failed: " + response.statusCode() + ", body=" + body);
//        }
    }

    public HttpRequest buildRequest( AIEngineRequest request) throws DBException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(AIHttpUtils.resolve(properties.getBaseUrl()))
                .header("Content-type", "application/json")
                .header("Authorization", "Bearer " + properties.getToken())
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(createRqMessage(request))))
                .timeout(TIMEOUT)
                .build();
        return httpRequest;
    }
    public BaseRequest createRqMessage(AIEngineRequest request) {
        if (!QwenConstants.DEFAULT_BASE_URL.equalsIgnoreCase(properties.getBaseUrl())){
            ArrayList<QwenRequestV2.Message> messages = new ArrayList<>();
            for (AIMessage message : request.getMessages()) {
                QwenRequestV2.Message qmessage = new QwenRequestV2.Message(
                        message.getRole().name().toLowerCase(),
                        message.getContent()
                );
                messages.add(qmessage);
            }
            QwenRequestV2 qwenRequest = new QwenRequestV2(
                    getModelName(),
                    messages,
                    properties.getTemperature(),
                    null
            );
            return qwenRequest;
        }
        ArrayList<QwenRequest.Message> messages = new ArrayList<>();
        for (AIMessage message : request.getMessages()) {
            QwenRequest.Message qmessage = new QwenRequest.Message(
                    message.getRole().name().toLowerCase(),
                    message.getContent()
            );
            messages.add(qmessage);
        }
        QwenRequest qwenRequest = new QwenRequest(
                getModelName(),
                messages,
                properties.getTemperature(),
                null
        );
        return qwenRequest;
    }



    @Override
    public void requestCompletionStream(DBRProgressMonitor monitor, AIEngineRequest request, AIEngineResponseConsumer listener) throws DBException {

    }

    @Override
    public int getContextWindowSize(DBRProgressMonitor monitor) throws DBException {
        Integer contextWindowSize = properties.getContextWindowSize();
        if (contextWindowSize != null) {
            return contextWindowSize;
        }

        throw new DBException("Context window size is not set for the model: ");
    }

    @Override
    public void close() throws DBException {
        client.dispose();
    }

    private String getModelName() {
        return CommonUtils.toString(
                properties.getModel(),
                QwenConstants.DEFAULT_MODEL
        );
    }
}