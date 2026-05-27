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

// QwenProperties.java
package org.jkiss.dbeaver.model.ai.engine.qwen;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.SecureProperty;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class QwenProperties implements AIEngineProperties {
    
    @Nullable
    private String baseUrl;
    
    @Nullable
    @SecureProperty
    private String token;
    
    @Nullable
    private String model;
    
    @Nullable
    private Integer contextWindowSize;
    
    private Double temperature;
    private Boolean loggingEnabled;

    public QwenProperties() {
    }

    @Nullable
    @Property(order = 1, id = QwenConstants.QWEN_BASE_URL)
    public String getBaseUrl() {
        if (baseUrl != null) {
            return baseUrl;
        }
        return QwenConstants.DEFAULT_BASE_URL;
    }

    public void setBaseUrl(@Nullable String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Nullable
    @Property(order = 2, id = QwenConstants.QWEN_API_TOKEN, password = true)
    public String getToken() {
        return token;
    }

    public void setToken(@Nullable String token) {
        this.token = token;
    }

    @Property(order = 3, id = QwenConstants.QWEN_MODEL, listProvider = QwenModelListProvider.class)
    public String getModel() {
        if (model != null) {
            return model;
        }
        return QwenConstants.DEFAULT_MODEL;
    }

    public void setModel(@Nullable String model) {
        this.model = model;
    }

    @Property(order = 4, id = QwenConstants.QWEN_TEMPERATURE)
    public double getTemperature() {
        if (temperature != null) {
            return temperature;
        }
        return 0.7; // 默认温度值
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    @Override
    public boolean isValidConfiguration() {
        return !CommonUtils.isEmpty(getToken());
    }

    @Override
    @Property(order = 5)
    public boolean isLoggingEnabled() {
        if (loggingEnabled != null) {
            return loggingEnabled;
        }
        return DBWorkbench.getPlatform()
            .getPreferenceStore()
            .getBoolean(AIConstants.AI_LOG_QUERY);
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    @Nullable
    @Property(order = 6)
    public Integer getContextWindowSize() {
        if (contextWindowSize != null) {
            return contextWindowSize;
        }
        // Qwen 模型的上下文窗口大小
        return QwenModels.getModelByName(getModel())
            .map(AIModel::contextWindowSize)
            .orElse(128000); // 默认值
    }

    public void setContextWindowSize(@Nullable Integer contextWindowSize) {
        this.contextWindowSize = contextWindowSize;
    }

    @Override
    public void resolveSecrets() throws DBException {
        token = AIUtils.getSecretValueOrDefault(QwenConstants.QWEN_API_TOKEN, token);
    }

    @Override
    public void saveSecrets() throws DBException {
        if (token != null) {
            DBSSecretController.getGlobalSecretController().setPrivateSecretValue(QwenConstants.QWEN_API_TOKEN, token);
        }
    }

    public static class QwenModelListProvider implements IPropertyValueListProvider<QwenProperties> {
        @Override
        public boolean allowCustomValue() {
            return true;
        }

        @Override
        public Object[] getPossibleValues(QwenProperties object) {
            return QwenModels.KNOWN_MODELS.keySet().toArray();
        }
    }
}