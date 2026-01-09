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
package org.jkiss.dbeaver.ext.xugu.tools;

import org.eclipse.ui.internal.dialogs.ExportWizard;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.xugu.DataSourceProvider;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Database;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.tasks.XuguTasks;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanelProvider;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.nativetool.NativeToolConfigPanel;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

import javax.xml.catalog.Catalog;

public class XuguTaskConfigurator implements DBTTaskConfigurator, DBTTaskConfigPanelProvider {

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public TaskConfigurationWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        switch (taskConfiguration.getType().getId()) {
            case XuguTasks.TASK_EXPORT_TOOL:
                return new XuguExportWizard(taskConfiguration);
        }
        return null;
    }

    private static class ConfigPanel extends NativeToolConfigPanel<Schema> {
        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            super(runnableContext, taskType, Schema.class, DataSourceProvider.class);
        }
    }

}
