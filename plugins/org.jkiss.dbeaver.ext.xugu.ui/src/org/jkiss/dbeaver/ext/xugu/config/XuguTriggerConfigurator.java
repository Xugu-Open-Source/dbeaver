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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.edit.WarningDialog;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.Trigger;
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
 * Trigger配置
 */
public class XuguTriggerConfigurator implements DBEObjectConfigurator<Trigger> {

    private static final Log log = Log.getLog(XuguTriggerConfigurator.class);

    @Override
    public Trigger configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                   @Nullable Object container, @NotNull Trigger trigger, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            if (trigger.getTable().isPersisted()) {
                return new UITask<Trigger>() {
                    @Override
                    protected Trigger runTask() {
                        TriggerDialog dialog = new TriggerDialog(UIUtils.getActiveWorkbenchShell(), monitor, trigger);
                        if (dialog.open() != IDialogConstants.OK_ID) {
                            return null;
                        }
                        return dialog.trigger;
                    }
                }.execute();
            } else {
                return new UITask<Trigger>() {
                    @Override
                    protected Trigger runTask() {
                        WarningDialog dialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "You must create table first");
                        if (dialog.open() != IDialogConstants.OK_ID) {
                            return null;
                        }
                        return null;
                    }
                }.execute();
            }
        });
    }

    static class TriggerDialog extends Dialog {
        private DBRProgressMonitor monitor;
        private Trigger trigger;
        private BaseTable baseTable;
        private Text nameText;
        private Text objectTypeText;
        private Text objectNameText;
        private Combo triggerTypeCombo;
        private Button triggerEventInsert;
        private Button triggerEventUpdate;
        private Button triggerEventDelete;
        private Combo triggerTimingCombo;
        private Text triggerConditionText;
        private Table colListTable;
        private Collection<? extends DBSEntityAttribute> colList;

        public TriggerDialog(Shell parentShell, DBRProgressMonitor monitor, Trigger trigger) {
            super(parentShell);
            this.monitor = monitor;
            this.baseTable = trigger.getTable();
            this.trigger = trigger;
            colList = new ArrayList<>();
        }

        public Trigger getTrigger() {
            return trigger;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(450, 550);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            String[] tableHeader = {"列名", "数据类型", "精度", "标度", "默认值"};
            getShell().setText(Messages.dialog_trigger_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 1, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, Messages.dialog_trigger_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            nameText.setEditable(true);

            objectTypeText = UIUtils.createLabelText(composite, Messages.dialog_trigger_parent_type, null);
            objectTypeText.setEditable(false);
            objectTypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (baseTable instanceof org.jkiss.dbeaver.ext.xugu.model.Table) {
                objectTypeText.setText("表");
            } else if (baseTable instanceof org.jkiss.dbeaver.ext.xugu.model.View) {
                objectTypeText.setText("视图");
            }

            objectNameText = UIUtils.createLabelText(composite, Messages.dialog_trigger_parent_name, null);
            objectNameText.setEditable(false);
            objectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            objectNameText.setText(baseTable.getName());

            Composite eventBox = UIUtils.createPlaceholder(composite, 4, 1);
            eventBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Control eventLabel = UIUtils.createInfoLabel(eventBox, Messages.dialog_trigger_event);
            eventLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventInsert = UIUtils.createCheckbox(eventBox, Messages.dialog_trigger_event_insert, false);
            triggerEventInsert.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventUpdate = UIUtils.createCheckbox(eventBox, Messages.dialog_trigger_event_update, false);
            triggerEventUpdate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerEventDelete = UIUtils.createCheckbox(eventBox, Messages.dialog_trigger_event_delete, false);
            triggerEventDelete.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            triggerTypeCombo = UIUtils.createLabelCombo(composite, Messages.dialog_trigger_type, 0);
            triggerTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTypeCombo.add(Messages.dialog_trigger_type_row);
            triggerTypeCombo.add(Messages.dialog_trigger_type_statement);
            triggerTypeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (triggerTypeCombo.getSelectionIndex() == 0 && !"INSTEAD OF".equals(triggerTimingCombo.getText())) {
                        // 元祖(行)级触发
                        triggerConditionText.setEditable(true);
                    } else {
                        // 语句级触发
                        triggerConditionText.setText("");
                        triggerConditionText.setEditable(false);
                    }
                }
            });

            triggerTimingCombo = UIUtils.createLabelCombo(composite, Messages.dialog_trigger_timing, 0);
            triggerTimingCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerTimingCombo.add("BEFORE");
            triggerTimingCombo.add("AFTER");
            triggerTimingCombo.add("INSTEAD OF");
            // 当创建视图触发器时，不展示timing界面
            if ("视图".equals(objectTypeText.getText())) {
                triggerTimingCombo.setText("INSTEAD OF");
                triggerTimingCombo.setEnabled(false);
            }

            triggerConditionText = UIUtils.createLabelText(composite, Messages.dialog_trigger_condition, null);
            triggerConditionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            triggerConditionText.setEditable(false);

            colListTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
            colListTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            colListTable.setHeaderVisible(true);
            colListTable.setLinesVisible(true);
            for (int i = 0; i < tableHeader.length; i++) {
                org.eclipse.swt.widgets.TableColumn tableColumn = new org.eclipse.swt.widgets.TableColumn(colListTable, SWT.NONE);
                tableColumn.setText(tableHeader[i]);
                // 设置表头可移动，默认为false
                tableColumn.setMoveable(true);
            }
            // 动态加载所有列信息
            triggerEventUpdate.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (triggerEventUpdate.getSelection()) {
                        updateColumnTable(parent, tableHeader);
                    } else {
                        colListTable.removeAll();
                    }
                }
            });
            UIUtils.createInfoLabel(composite, Messages.dialog_trigger_label, GridData.FILL_HORIZONTAL, 2);
            return parent;
        }

        private void updateColumnTable(Composite parent, String[] tableHeader) {
            try {
                colList = baseTable.getAttributes(monitor);
            } catch (DBException e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            // colList = schema.getTable(monitor, objName).getAttributes(monitor);
            // 重新加载数据
            colListTable.removeAll();
            if (colList.size() != 0) {
                Iterator<? extends DBSEntityAttribute> it = colList.iterator();
                while (it.hasNext()) {
                    org.jkiss.dbeaver.ext.xugu.model.TableColumn col = (org.jkiss.dbeaver.ext.xugu.model.TableColumn) it.next();
                    TableItem item = new TableItem(colListTable, SWT.NONE);
                    item.setText(new String[]{col.getName(),
                            col.getDataType() == null ? col.getTypeName() : col.getDataType().toString(),
                            col.getPrecision() == null ? "" : String.valueOf(col.getPrecision()),
                            col.getScale() == null ? "" : String.valueOf(col.getScale()), col.getDefaultValue()});
                }
            }
            // 调整表格大小
            for (int i = 0; i < tableHeader.length; i++) {
                colListTable.getColumn(i).pack();
            }
        }

        @Override
        protected void okPressed() {
            if (!Utils.checkString(nameText.getText())) {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        Messages.dialog_trigger_name_warn);
                warnDialog.open();
                return;
            }
            if (!triggerEventInsert.getSelection() && !triggerEventUpdate.getSelection()
                    && !triggerEventDelete.getSelection()) {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        Messages.dialog_trigger_event_warn);
                warnDialog.open();
                return;
            }
            if (triggerTypeCombo.getSelectionIndex() == -1) {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        Messages.dialog_trigger_type_warn);
                warnDialog.open();
                return;
            }
            if (!Utils.checkString(triggerTimingCombo.getText())) {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        Messages.dialog_trigger_timing_warn);
                warnDialog.open();
                return;
            }
            if (triggerTypeCombo.getSelectionIndex() == 0
                    && !"INSTEAD OF".equals(triggerTimingCombo.getText())
                    && !Utils.checkString(triggerConditionText.getText())) {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        Messages.dialog_trigger_condition_warn);
                warnDialog.open();
                return;
            }
            String source = "\nBEGIN\n\nEND";
            // 设置父对象信息
            trigger.setName(DBObjectNameCaseTransformer.transformObjectName(trigger, nameText.getText()));
            // 当创建视图触发器时，timing自动设为instead of
            if ("视图".equals(objectTypeText.getText())) {
                trigger.setObjectType("VIEW");
                trigger.setTriggerTime(2);
            } else {
                trigger.setObjectType("TABLE");
                trigger.setTriggerTime(triggerTimingCombo.getText());
            }

            trigger.setTriggerCondition(triggerConditionText.getText());
            if (triggerTypeCombo.getSelectionIndex() > -1) {
                trigger.setTriggerType(triggerTypeCombo.getSelectionIndex() + 1);
            }
            int event = 0;
            if (triggerEventInsert.getSelection()) {
                event += 1;
            }
            if (triggerEventUpdate.getSelection()) {
                event += 2;
            }
            if (triggerEventDelete.getSelection()) {
                event += 4;
            }
            trigger.setTriggeringEvent(event);
            trigger.setObjectDefinitionText(source);
            // 加载列信息
            TableItem[] cols = colListTable.getItems();
            if (cols != null) {
                int sum = cols.length;
                int i = 0;
                ArrayList<String> includeCols = new ArrayList<String>();
                while (i < sum) {
                    if (cols[i].getChecked()) {
                        includeCols.add(cols[i].getText(0));
                    }
                    i++;
                }
                trigger.setIncludeColumns(includeCols);
            }
            super.okPressed();
        }
    }
}
