/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.engine.qwen;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.utils.MonitoredHttpClient;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QwenClient implements AutoCloseable {
    private static final Log log = Log.getLog(QwenClient.class);

    protected final String baseUrl;
    protected final String requestFilters;

    protected final MonitoredHttpClient client = new MonitoredHttpClient(
        HttpClient.newBuilder().build(),
        this::mapHttpError,
        this::processErrors
    );

    public QwenClient(
            @NotNull String baseUrl,
            @NotNull String token
    ) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        this.baseUrl = baseUrl;
        this.requestFilters = token;
    }
    @Override
    public void close()  {
        client.close();
    }

    public static QwenClient createClient(String baseUrl, String token) {
        return new QwenClient(
                baseUrl,
                token
        );
    }

    @NotNull
    protected DBException mapHttpError(int statusCode, @NotNull String body) {
        log.debug("Qwen request failed: " + statusCode + ", " + body);
        return new DBException("Qwen request failed: " + statusCode + ", body=" + body);
    }

    protected boolean processErrors(
        @NotNull MonitoredHttpClient.ErrorMapper mapper,
        @NotNull Consumer<Throwable> errorHandler,
        @NotNull HttpResponse<Stream<String>> response,
        @NotNull AtomicBoolean suppressCompletion,
        @Nullable Runnable backupOption,
        int statusCode
    ) {
        if (statusCode != 200) {
            String responseBody = response.body().collect(Collectors.joining());
            errorHandler.accept(mapper.map(statusCode, responseBody));
            return true;
        }
        return false;
    }
}
