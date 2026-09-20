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

// QwenModels.java
package org.jkiss.dbeaver.model.ai.engine.qwen;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class QwenModels {
    private QwenModels() {
    }

    public static final Map<String, AIModel> KNOWN_MODELS = Stream.of(
        new AIModel("qwen-turbo", 128000, Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING)),
        new AIModel("qwen-plus", 1024000, Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING)),
        new AIModel("qwen-max", 32000, Set.of(AIModelFeature.CHAT, AIModelFeature.STREAMING))
    ).collect(Collectors.toMap(
        AIModel::name,
        Function.identity()
    ));

    @NotNull
    public static Optional<AIModel> getModelByName(@Nullable String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(KNOWN_MODELS.get(modelName.toLowerCase(Locale.ROOT)));
    }

    public static Set<AIModelFeature> detectModelFeatures(@NotNull String modelName) {
        AIModel knownModel = KNOWN_MODELS.get(modelName.toLowerCase(Locale.ROOT));
        if (knownModel != null) {
            return knownModel.features();
        }
        
        // 如果模型不在已知列表中，默认为聊天模型
        Set<AIModelFeature> features = new HashSet<>();
        features.add(AIModelFeature.CHAT);
        features.add(AIModelFeature.STREAMING);
        return features;
    }
}