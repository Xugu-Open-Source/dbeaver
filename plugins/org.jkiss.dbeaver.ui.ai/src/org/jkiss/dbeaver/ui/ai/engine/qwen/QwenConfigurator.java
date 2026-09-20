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
package org.jkiss.dbeaver.ui.ai.engine.qwen;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.qwen.QwenConstants;
import org.jkiss.dbeaver.model.ai.engine.qwen.QwenModels;
import org.jkiss.dbeaver.model.ai.engine.qwen.QwenProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.model.ContextWindowSizeField;
import org.jkiss.dbeaver.ui.ai.model.ModelSelectorField;
import org.jkiss.dbeaver.ui.ai.preferences.AIIObjectPropertyConfigurator;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

public class QwenConfigurator implements AIIObjectPropertyConfigurator<AIEngineDescriptor, QwenProperties> {
    private static final String QWEN_API_KEY_URL = "https://bailian.console.aliyun.com/";

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
        descriptionText = UIUtils.createInfoLabel(
                parent,
                "此选项支持硅基和百炼的模型"
        );

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        descriptionText.setLayoutData(gd);
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
    public void loadSettings(@NotNull QwenProperties configuration) {
        baseUrl = CommonUtils.toString(configuration.getBaseUrl(), QwenConstants.DEFAULT_BASE_URL);
        token = CommonUtils.toString(configuration.getToken());

        String model = CommonUtils.toString(configuration.getModel(), QwenConstants.DEFAULT_MODEL);
        if (modelSelectorField != null) {
            modelSelectorField.setSelectedModel(model);
        }

        temperature = String.valueOf(configuration.getTemperature());
        logQuery = configuration.isLoggingEnabled();
        applySettings();

        if (contextWindowSizeField != null) {
            contextWindowSizeField.setValue(configuration.getContextWindowSize());
        }

        if (modelSelectorField != null) {
            modelSelectorField.refreshModelListSilently(false);
        }
    }

    @Override
    public void saveSettings(@NotNull QwenProperties configuration) {
        configuration.setBaseUrl(baseUrl);
        configuration.setToken(token);

        if (modelSelectorField != null) {
            configuration.setModel(modelSelectorField.getSelectedModelName());
        }

        if (contextWindowSizeField != null) {
            configuration.setContextWindowSize(contextWindowSizeField.getValue());
        }

        configuration.setTemperature(CommonUtils.toDouble(temperature));
        configuration.setLoggingEnabled(logQuery);
    }

    @Override
    public void resetSettings(@NotNull QwenProperties properties) {
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
            return new ArrayList<>(QwenModels.KNOWN_MODELS.values());
        };
        modelSelectorField = ModelSelectorField.builder()
            .withParent(parent)
            .withGridData(new GridData(GridData.FILL_HORIZONTAL))
            .withModelListSupplier(modelListProvider)
            .withModifyListener(() -> {
                AIModel selectedModel = modelSelectorField.getSelectedModel();
                if (selectedModel != null) {
                    if (contextWindowSizeField != null && selectedModel.contextWindowSize() != null) {
                        contextWindowSizeField.setValue(selectedModel.contextWindowSize());
                    }
                    if (temperatureText != null) {
                        temperatureText.setText(String.valueOf(selectedModel.defaultTemperature() != 0.0
                            ? selectedModel.defaultTemperature()
                            : 0.7));
                    }
                }
            })
            .build();

        contextWindowSizeField = ContextWindowSizeField.builder()
            .withParent(parent)
            .withGridData(GridDataFactory.fillDefaults().span(2, 1).create())
            .build();

        temperatureText = UIUtils.createLabelText(parent, "Temperature", "0.7");
        temperatureText.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
        temperatureText.setLayoutData(GridDataFactory.fillDefaults().span(2, 1).create());

        temperatureText.setToolTipText("Lower temperatures give more precise results (0.0-1.0)");
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

        if (CommonUtils.isEmpty(baseUrl)) {
            baseUrl = QwenConstants.DEFAULT_BASE_URL;
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
        QwenProperties qwenProperties = new QwenProperties();
        saveSettings(qwenProperties);
        return Optional.of(qwenProperties);
    }
}
