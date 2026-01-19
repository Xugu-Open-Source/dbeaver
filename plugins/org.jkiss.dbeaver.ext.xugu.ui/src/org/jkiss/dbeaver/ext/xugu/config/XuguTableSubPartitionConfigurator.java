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
package org.jkiss.dbeaver.ext.xugu.config;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.BaseTablePhysical;
import org.jkiss.dbeaver.ext.xugu.model.TableSubPartition;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * 分区表配置
 */
public class XuguTableSubPartitionConfigurator implements DBEObjectConfigurator<TableSubPartition> {
    private static final Log log = Log.getLog(XuguTableSubPartitionConfigurator.class);
    @Override
    public TableSubPartition configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull TableSubPartition partition, @NotNull Map<String, Object> options) {
        BaseTablePhysical parent = (BaseTablePhysical) container;
        // 仅允许对新创建的表进行添加二级分区操作
        if (parent.isPersisted()) {
            return UITask.run(()->{
                WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Can't create sub partition on existed table");
                if (dialog2.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                return null;
            });
        }
        // 禁止对没有分区的表添加子分区操作
        if (parent.partitionCache.isEmpty()) {
            return UITask.run(()->{
                WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Can't create new partition on table with no partition");
                if (dialog2.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                return null;
            });
        }

        return new UITask<TableSubPartition>() {
            @Override
            protected TableSubPartition runTask() {
                NewTablePartitionDialog dialog = new NewTablePartitionDialog(UIUtils.getActiveWorkbenchShell(),
                        monitor, parent);
                if (dialog.open() != IDialogConstants.OK_ID) {
                    return null;
                }
                TableSubPartition newTablePartition = dialog.getTablePartition();
                if (parent.isPersisted()) {
                    ArrayList<TableSubPartition> partList = (ArrayList<TableSubPartition>) parent.subPartitionCache
                            .getCachedObjects();
                    if (!partList.isEmpty()) {
                        TableSubPartition model = partList.getFirst();
                        newTablePartition.setPartiType(model.getPartiType());
                        newTablePartition.setPartiKey(model.getPartiKey());
                    }
                }
                if (newTablePartition.isSubPartition()) {
                    parent.subPartitionCache.cacheObject(newTablePartition);
                }
                return newTablePartition;
            }
        }.execute();
    }

    static class NewTablePartitionDialog extends Dialog {
        private TableSubPartition partition;
        private BaseTablePhysical table;
        private DBRProgressMonitor monitor;
        private Text nameText;
        private Combo typeCombo;
        private Text valueText;
        private Combo colCombo;

        public NewTablePartitionDialog(Shell parentShell, DBRProgressMonitor monitor, BaseTablePhysical table) {
            super(parentShell);
            this.table = table;
            this.monitor = monitor;
        }

        public TableSubPartition getTablePartition() {
            return this.partition;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(300, 250);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            getShell().setText(Messages.dialog_tablePartition_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, Messages.dialog_tablePartition_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            typeCombo = UIUtils.createLabelCombo(composite, "Type", 8);
            typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            typeCombo.add("LIST");
            typeCombo.add("RANGE");
            typeCombo.add("HASH");

            valueText = UIUtils.createLabelText(composite, Messages.dialog_tablePartition_value, null);
            valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            colCombo = UIUtils.createLabelCombo(composite, "column", 8);
            colCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            try {
                Collection<? extends DBSEntityAttribute> cols = table.getAttributes(monitor);
                if (cols != null) {
                    Iterator<? extends DBSEntityAttribute> it = cols.iterator();
                    while (it.hasNext()) {
                        String name = it.next().getName();
                        colCombo.add(name);
                    }
                }
            } catch (DBException e) {
                log.error(e.getMessage(), e);
            }

            UIUtils.createInfoLabel(composite, Messages.dialog_tablePartition_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed() {
            if (Utils.checkString(nameText.getText())) {
                this.partition = new TableSubPartition(table, false, "");
                partition.setName(DBObjectNameCaseTransformer.transformObjectName(partition, nameText.getText()));
                partition.setPartiType(DBObjectNameCaseTransformer.transformObjectName(partition, typeCombo.getText()));
                partition
                        .setPartiValue(DBObjectNameCaseTransformer.transformObjectName(partition, valueText.getText()));
                partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, colCombo.getText()));
                partition.setSubPartition(true);
                partition.setOnline(true);
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(
                        UIUtils.getActiveWorkbenchShell(), "Table Subpartition name cannot be null");
                warnDialog.open();
            }
        }

    }

     static class WarningDialog extends Dialog {
        private String warningInfo;

        public WarningDialog(Shell parentShell, String info) {
            super(parentShell);
            this.warningInfo = info;
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            getShell().setText(Messages.dialog_tablePartition_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            Label infoText = UIUtils.createLabel(composite, "Warning: " + this.warningInfo);
            infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }
    }
}
