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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;

import org.jkiss.dbeaver.ext.xugu.tasks.DatabaseExportInfo;
import org.jkiss.dbeaver.ext.xugu.tasks.XuguExportSettings;
import org.jkiss.dbeaver.ext.xugu.tasks.XuguTasks;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeExportWizard;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class XuguExportWizard extends AbstractNativeExportWizard<XuguExportSettings, DatabaseExportInfo> {
    private XuguExportWizardPageObjects objectsPage;
    private XuguExportWizardPageSettings settingsPage;

    XuguExportWizard(Collection<DBSObject> objects) {
        super(objects, "XuGuDB导出数据库");
        getSettings().fillExportObjectsFromInput();
    }

    XuguExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    protected XuguExportSettings createSettings() {
        return new XuguExportSettings();
    }

    @Override
    public String getTaskTypeId() {
        return XuguTasks.TASK_EXPORT_TOOL;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        if (objectsPage.getControl() != null) {
            objectsPage.saveState();
        }
        if (settingsPage.getControl() != null) {
            settingsPage.saveState();
        }
        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        objectsPage = new XuguExportWizardPageObjects(this);
        settingsPage = new XuguExportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addTaskConfigPages();
        addPage(objectsPage);
        addPage(settingsPage);
        super.addPages();
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == settingsPage) {
            return null;
        }
        return super.getNextPage(page);
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        if (page == logPage) {
            return settingsPage;
        }
        return super.getPreviousPage(page);
    }

    @Override
	public void onSuccess(long workTime) {
        UIUtils.showMessageBox(
            getShell(),
            "导出数据库",
            CommonUtils.truncateString(NLS.bind("数据库 \"{0}\" 导出完成", getObjectsName()), 255),
            SWT.ICON_INFORMATION);

        Set<String> set = getSettings().getExportObjects().stream().map(it -> getSettings().getOutputFolder(it)).collect(Collectors.toSet());
        set.forEach(ShellUtils::launchProgram);
	}
}
