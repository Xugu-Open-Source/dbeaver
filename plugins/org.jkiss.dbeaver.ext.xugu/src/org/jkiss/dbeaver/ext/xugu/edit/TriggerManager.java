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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.model.ObjectType;
import org.jkiss.dbeaver.ext.xugu.model.ObjectValidateAction;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.Trigger;

import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
/**
 * 触发器管理器，进行触发器的创建和删除，修改相当于创建并替换，包含一个内部界面类，用于进行属性设定
 */
public class TriggerManager extends SQLTriggerManager<Trigger, BaseTable> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Trigger> getObjectsCache(Trigger object) {
		return object.getTable().triggerCache;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Trigger name cannot be empty");
		}
	}

	@Override
	public boolean canCreateObject(Object container) {
		return container instanceof Schema;
	}

	@Override
	protected Trigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		Schema parent = (Schema) container;
		if (parent.isPersisted()) {
			return new UITask<Trigger>() {
				@Override
				protected Trigger runTask() {
					TriggerDialog dialog = new TriggerDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
					if (dialog.open() != IDialogConstants.OK_ID) {
						return null;
					}
					Trigger newTrigger = dialog.getTrigger();
					return newTrigger;
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
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Trigger, BaseTable>.ObjectCreateCommand command,
			Map<String, Object> options) {
		createOrReplaceTriggerQuery(actions, command.getObject());
	}

	@Override
	protected void createOrReplaceTriggerQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, Trigger trigger, boolean create) {
		// TODO 创建或替换触发器查询
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Trigger, BaseTable>.ObjectChangeCommand command,
			Map<String, Object> options) {
		final String objectDefKey = "objectDefinitionText";
		final String triggerConditionKey = "triggerCondition";

		if (command.getProperty(objectDefKey) != null || command.getProperty(triggerConditionKey) != null) {
			createOrReplaceTriggerQuery(actionList, command.getObject());
		}
	}

	/**
	 * 修改模式名称
	 */
	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Trigger, BaseTable>.ObjectRenameCommand command,
			Map<String, Object> options) {
		StringBuilder desc = new StringBuilder(100);
		if (!CommonUtils.isEmpty(command.getNewName())) {
			desc.append("ALTER TRIGGER ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()));
			desc.append(" RENAME TO ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(),
					command.getNewName().toUpperCase()));
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct rename schema sql: " + desc.toString());
		actionList.add(new SQLDatabasePersistAction("rename schema", desc.toString()));
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<Trigger, SQLObjectEditor<Trigger, BaseTable>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON TRIGGER ");
			desc.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add trigger comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment Trigger", desc.toString()));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Trigger, BaseTable>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop trigger sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop trigger", sql));
	}

	protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, Trigger trigger) {
		String source = Utils.normalizeSourceName(trigger, false);
		try {
			source = trigger.getExtendedDefinitionText(new VoidProgressMonitor());
		} catch (DBException e) {
			log.error(e);
		}
		final String codeBody = "\nBEGIN\n\nEND";
		if (source == null || !Utils.checkString(source) || source.equals(codeBody)) {
			actions.add(new ObjectValidateAction(trigger, ObjectType.TRIGGER, "Create trigger action", source));
		} else {
			String event = trigger.getTriggeringEvent();
			String timing = trigger.getTriggerTime();
			String type = trigger.getTriggerType();
			List<String> includeCols = trigger.getIncludeColumns();
			String targetCols = "";
			if (includeCols != null) {
				for (String str : includeCols) {
					targetCols += "\"" + str + "\"" + ",";
				}
				if (!"".equals(targetCols)) {
					targetCols = targetCols.substring(0, targetCols.length() - 1);
					targetCols = " OF " + targetCols;
				}
			}
			// 处理触发器事件字段
			final String separator = ",";
			if (event.indexOf(separator) != -1) {
				event = event.replaceAll(",", " OR ");
			}
			String condition = trigger.getTriggerCondition();
			String realCondition = condition != null ? "".equals(condition) ? null : condition : null;
			source = "CREATE OR REPLACE TRIGGER " + trigger.getFullyQualifiedName(DBPEvaluationContext.DDL) + " \n"
					+ timing + " " + event + targetCols + " ON "
					+ trigger.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " \n" + type
					+ ("FOR EACH ROW".equals(trigger.getTriggerType()) ? " WHEN(" + realCondition + ") \n" : " \n")
					+ source;

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create trigger sql: " + source);
			actions.add(new SQLDatabasePersistAction("Create trigger", source, true));
			// trigger.setPersisted(true);
		}
	}

	static class TriggerDialog extends Dialog {
		private DBRProgressMonitor monitor;
		private Trigger trigger;
		private Schema schema;
		private Text nameText;
		private Combo objectTypeCombo;
		private Combo objectNameCombo;
		private Combo triggerTypeCombo;
		private Button triggerEventInsert;
		private Button triggerEventUpdate;
		private Button triggerEventDelete;
		private Combo triggerTimingCombo;
		private Text triggerConditionText;
		private Table colListTable;
		private Collection<? extends DBSEntityAttribute> colList;

		public  TriggerDialog(Shell parentShell, Schema schema, DBRProgressMonitor monitor) {
			super(parentShell);
			this.monitor = monitor;
			this.schema = schema;
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
			String[] tableHeader = { "列名", "数据类型", "精度", "标度", "默认值" };
			getShell().setText(Messages.dialog_trigger_create_title);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 1, 5);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			nameText = UIUtils.createLabelText(composite, Messages.dialog_trigger_name, null);
			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			nameText.setEditable(true);

			objectTypeCombo = UIUtils.createLabelCombo(composite, Messages.dialog_trigger_parent_type, 0);
			objectTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			objectTypeCombo.add("表");
			objectTypeCombo.add("视图");
			objectTypeCombo.select(0);
			objectTypeCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					try {
						switch (objectTypeCombo.getSelectionIndex()) {
						case 0:
							objectNameCombo.removeAll();
							schema.getTables(monitor).forEach(table -> objectNameCombo.add(table.getName()));
							objectNameCombo.select(0);
							break;
						case 1:
							objectNameCombo.removeAll();
							schema.getViews(monitor).forEach(view -> objectNameCombo.add(view.getName()));
							objectNameCombo.select(0);
							break;
						}
					} catch (DBException ex) {
						MessageDialog.openError(getShell(), "获取对象名称失败", ex.getLocalizedMessage());
						ex.printStackTrace();
					}

					if (triggerEventUpdate.getSelection()) {
						updateColumnTable(parent, tableHeader);
					}
				}
			});

			objectNameCombo = UIUtils.createLabelCombo(composite, Messages.dialog_trigger_parent_name, 0);
			objectNameCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			try {
				schema.getTables(monitor).forEach(table -> objectNameCombo.add(table.getName()));
			} catch (DBException ex) {
				MessageDialog.openError(parent.getShell(), "获取对象名称失败", ex.getLocalizedMessage());
				ex.printStackTrace();
			}
			objectNameCombo.select(0);
			objectNameCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (triggerEventUpdate.getSelection()) {
						updateColumnTable(parent, tableHeader);
					}
				}
			});

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
					if (triggerTypeCombo.getSelectionIndex() == 0) {
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
			if ("视图".equals(objectTypeCombo.getText())) {
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
				TableColumn tableColumn = new TableColumn(colListTable, SWT.NONE);
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
			String objName = objectNameCombo.getText();

			switch (objectTypeCombo.getSelectionIndex()) {
			case 0:
				try {
					colList = schema.getTable(monitor, objName).getAttributes(monitor);
				} catch (DBException ex) {
					MessageDialog.openError(parent.getShell(), "获取表对象失败", ex.getLocalizedMessage());
					ex.printStackTrace();
				}
				break;
			case 1:
				try {
					colList = schema.getView(monitor, objName).getAttributes(monitor);
				} catch (DBException ex) {
					MessageDialog.openError(parent.getShell(), "获取视图对象失败", ex.getLocalizedMessage());
					ex.printStackTrace();
					return;
				}
				break;
			}

			// 重新加载数据
			colListTable.removeAll();
			if (colList.size() != 0) {
				Iterator<? extends DBSEntityAttribute> it = colList.iterator();
				while (it.hasNext()) {
					org.jkiss.dbeaver.ext.xugu.model.TableColumn col = (org.jkiss.dbeaver.ext.xugu.model.TableColumn) it.next();
					TableItem item = new TableItem(colListTable, SWT.NONE);
					item.setText(new String[] { col.getName(),
							col.getDataType() == null ? col.getTypeName() : col.getDataType().toString(),
							col.getPrecision() == null ? "" : String.valueOf(col.getPrecision()),
							col.getScale() == null ? "" : String.valueOf(col.getScale()), col.getDefaultValue() });
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
			if (triggerTypeCombo.getSelectionIndex() == 0 && !Utils.checkString(triggerConditionText.getText())) {
				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
						Messages.dialog_trigger_condition_warn);
				warnDialog.open();
				return;
			}
			String source = "\nBEGIN\n\nEND";
			// 设置父对象信息
			
			try {
				switch (objectTypeCombo.getSelectionIndex()) {
				case 0:
					this.trigger = new Trigger(schema.getTable(monitor, objectNameCombo.getText()), "");
					break;
				case 1:
					this.trigger = new Trigger(schema.getView(monitor, objectNameCombo.getText()), "");
					break;
				}
			} catch (DBException ex) {
				MessageDialog.openError(this.getShell(), "获取对象名称失败", ex.getLocalizedMessage());
				ex.printStackTrace();
			}
			
			trigger.setName(DBObjectNameCaseTransformer.transformObjectName(trigger, nameText.getText()));
			// 当创建视图触发器时，timing自动设为instead of
			if ("视图".equals(objectTypeCombo.getText())) {
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
