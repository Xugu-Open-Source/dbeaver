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
package org.jkiss.dbeaver.ui.ai.engine.qwen;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.qwen.QwenProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AIIObjectPropertyConfigurator;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class QwenConfigurator implements AIIObjectPropertyConfigurator<AIEngineDescriptor, AIEngineProperties> {
    private static final String QWEN_API_KEY_URL = "https://bailian.console.aliyun.com/";
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private static final String DEFAULT_MODEL = "qwen-max";
    
    protected String baseUrl;
    protected volatile String token = "";
    private String temperature = "0.7";
    private boolean logQuery = false;

    @Nullable
    private Text baseUrlText;

    @Nullable
    protected Text tokenText;
    private Text temperatureText;
    private ModelSelectorField modelSelectorField;
    private ContextWindowSizeField contextWindowSizeField;
    private Button logQueryCheck;

    @Nullable
    private Control descriptionText;

    protected void createDescriptionLabel(@NotNull Composite parent) {
        // 创建只读的文本标签
        descriptionText = UIUtils.createInfoLabel(
                parent,
                "此选项支持硅基和百炼的模型"
        );

        // 设置为只读
//        descriptionText.setEditable(false);

        // 设置布局数据
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;  // 跨越3列，与其他控件对齐
        descriptionText.setLayoutData(gd);

        // 可以设置背景色来表明这是只读的
        descriptionText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }
    @Override
    public void createControl(
        @NotNull Composite parent,
        AIEngineDescriptor object,
        @NotNull Runnable propertyChangeListener
    ) {
        Composite composite = UIUtils.createComposite(parent, 3);
        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createConnectionParameters(composite);
        createModelParameters(composite);
        createBaseUrlParameter(composite);
        createAdditionalSettings(composite);
        createDescriptionLabel(composite);
    }

    @Override
    public void loadSettings(@NotNull AIEngineProperties configuration) {
        // 使用反射或直接处理属性
        baseUrl = getProperty(configuration, "baseUrl", DEFAULT_BASE_URL);
        token = getProperty(configuration, "token", "");
        
        String model = getProperty(configuration, "model", DEFAULT_MODEL);
        if (modelSelectorField != null) {
            modelSelectorField.setSelectedModel(model);
        }
        
        temperature = getProperty(configuration, "temperature", "0.7");
        logQuery = Boolean.parseBoolean(getProperty(configuration, "loggingEnabled", "false"));
        applySettings();

        String contextWindowSize = getProperty(configuration, "contextWindowSize", null);
        if (contextWindowSize != null && contextWindowSizeField != null) {
            contextWindowSizeField.setValue(Integer.parseInt(contextWindowSize));
        }

        if (modelSelectorField != null) {
            modelSelectorField.refreshModelListSilently(false);
        }
    }

    @Override
    public void saveSettings(@NotNull AIEngineProperties configuration) {
        // 使用反射设置属性
        setProperty(configuration, "baseUrl", baseUrl);
        setProperty(configuration, "token", token);
        
        if (modelSelectorField != null) {
            setProperty(configuration, "model", modelSelectorField.getSelectedModel());
        }
        
        if (contextWindowSizeField != null) {
            setProperty(configuration, "contextWindowSize", 
                contextWindowSizeField.getValue() != null ? 
                String.valueOf(contextWindowSizeField.getValue()) : null);
        }
        
        setProperty(configuration, "temperature", temperature);
        setProperty(configuration, "loggingEnabled", String.valueOf(logQuery));
    }

    @Override
    public void resetSettings(@NotNull AIEngineProperties properties) {
        // Reset to default values if needed
    }

    protected void createAdditionalSettings(@NotNull Composite parent) {
        logQueryCheck = UIUtils.createCheckbox(
            parent,
            "Write AI queries to debug log",
            "Write AI queries with metadata info in debug logs",
            false,
            2
        );
        logQueryCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                logQuery = logQueryCheck.getSelection();
            }
        });
    }

    protected void createModelParameters(@NotNull Composite parent) {
        ModelSelectorField.ModelListProvider modelListProvider = (monitor, forceRefresh) -> {
            if (token == null || token.isEmpty()) {
                throw new DBException("API Key is not set");
            }
//            return QwenModels.KNOWN_MODELS.entrySet().stream().map(entry -> entry.getKey()).toList(); todo
            return List.of("qwen-max", "qwen-turbo", "qwen-plus");
        };

        modelSelectorField = ModelSelectorField.builder()
            .withParent(parent)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withModelListSupplier(modelListProvider)
            .withSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
                // 根据选择的模型设置默认值
                String selectedModel = modelSelectorField.getSelectedModel();
                if (selectedModel != null) {
                    // 设置默认上下文窗口大小
                    if (contextWindowSizeField != null) {
                        contextWindowSizeField.setValue(128000); // Qwen 模型的默认值
                    }
                    // 设置默认温度
                    if (temperatureText != null) {
                        temperatureText.setText("0.7");
                    }
                }
            }))
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(parent, "Temperature", "0.7");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());

        temperatureText.setToolTipText("Lower temperatures give more precise results (0.0-1.0)");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.addModifyListener((e) -> temperature = temperatureText.getText());
    }

    protected void createConnectionParameters(@NotNull Composite parent) {
        tokenText = UIUtils.createLabelText(
            parent,
            "Qwen API Key",
            "",
            SWT.BORDER | SWT.PASSWORD
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        tokenText.setLayoutData(gd);
        tokenText.addModifyListener((e -> token = tokenText.getText()));
        tokenText.setMessage("Enter your Qwen API Key");
        createURLInfoLink(parent);
//        createURLInfoLink2(parent);
    }

    protected void createBaseUrlParameter(@NotNull Composite parent) {
        baseUrlText = UIUtils.createLabelText(
            parent,
            "Base URL",
            ""
        );
        baseUrlText.addModifyListener((e -> baseUrl = baseUrlText.getText()));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 150;
        baseUrlText.setLayoutData(gd);
        
        // Set default base URL
        if (CommonUtils.isEmpty(baseUrl)) {
            baseUrl = DEFAULT_BASE_URL;
        }
        baseUrlText.setText(baseUrl);
    }

    protected void createURLInfoLink(@NotNull Composite parent) {
        Link link = UIUtils.createLink(
            parent,
            NLS.bind("获取 百炼key <a>{0}</a> ", getApiKeyURL()),
            new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.openWebBrowser(getApiKeyURL());
                }
            }
        );
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        link.setLayoutData(gd);
    }
//    protected void createURLInfoLink2(@NotNull Composite parent) {
//        Link link = UIUtils.createLink(
//                parent,
//                NLS.bind("获取 硅基流动key <a>{0}</a> ", "https://cloud.siliconflow.cn/me/models"),
//                new SelectionAdapter() {
//                    @Override
//                    public void widgetSelected(SelectionEvent e) {
//                        UIUtils.openWebBrowser("https://cloud.siliconflow.cn/me/models");
//                    }
//                }
//        );
//        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
//        gd.horizontalSpan = 3;
//        link.setLayoutData(gd);
//    }

    protected String getApiKeyURL() {
        return QWEN_API_KEY_URL;
    }

    protected void applySettings() {
        if (baseUrlText != null) {
            baseUrlText.setText(baseUrl);
        }
        if (tokenText != null) {
            tokenText.setText(token);
        }

        if (temperatureText != null) {
            temperatureText.setText(temperature);
        }
        if (logQueryCheck != null) {
            logQueryCheck.setSelection(logQuery);
        }
    }

    @Override
    public boolean isComplete() {
        return tokenText != null
            && !tokenText.getText().isEmpty()
            && (contextWindowSizeField == null || contextWindowSizeField.isComplete());
    }

    @Override
    public Optional<AIEngineProperties> getCurrentProperties() {
        try {
            QwenProperties qwenProperties = new QwenProperties();
            // 使用反射创建 QwenProperties 实例
//            Class<?> qwenPropertiesClass = Class.forName("org.jkiss.dbeaver.model.ai.engine.Qwen.QwenProperties");
//            AIEngineProperties propertiesCopy = (AIEngineProperties) qwenPropertiesClass.newInstance();
            
            saveSettings(qwenProperties);
            return Optional.of(qwenProperties);
        } catch (Exception e) {
            // 如果反射失败，返回空
            return Optional.empty();
        }
    }

    // 辅助方法：使用反射获取属性
    private String getProperty(AIEngineProperties config, String propertyName, String defaultValue) {
        try {
            Method method = config.getClass().getMethod("get" + capitalize(propertyName));
            Object result = method.invoke(config);
            return result != null ? result.toString() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // 辅助方法：使用反射设置属性
    private void setProperty(AIEngineProperties config, String propertyName, String value) {
        try {
            Method method = config.getClass().getMethod("set" + capitalize(propertyName), String.class);
            method.invoke(config, value);
        } catch (Exception e) {
            // 忽略设置失败
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}