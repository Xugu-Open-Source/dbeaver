/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.TableColumn;
import org.jkiss.dbeaver.ext.xugu.model.TablePartition;
import org.jkiss.dbeaver.ext.xugu.model.BaseTablePhysical;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * 表分区管理器，进行表分区的创建和删除，修改仅支持设定是否在线，包含一个内部界面类，用于进行属性设定
 */
public class TablePartitionManager extends SQLObjectEditor<TablePartition, BaseTablePhysical> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	protected TablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		BaseTablePhysical parent = (BaseTablePhysical) container;
		// 禁止对没有分区定义的表进行添加分区操作
		if (parent.isPersisted() == true && parent.partitionCache.getCachedObjects().size() == 0) {
			new UITask<String>() {
				@Override
				protected String runTask() {
					WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Can't create new partition on table with no partition");
					if (dialog2.open() != IDialogConstants.OK_ID) {
						return null;
					}
					return null;
				}
			}.execute();
			return null;
		}

		// 禁止对没有列的表进行添加分区操作
		try {
			Collection<? extends DBSEntityAttribute> cols = parent.getAttributes(monitor);
			if (cols == null) {
				new UITask<String>() {
					@Override
					protected String runTask() {
						WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "You must add column first");
						if (dialog2.open() != IDialogConstants.OK_ID) {
							return null;
						}
						return null;
					}
				}.execute();
				return null;
			}
		} catch (DBException e) {
			e.printStackTrace();
		}

		return new UITask<TablePartition>() {
			@Override
			protected TablePartition runTask() {
				NewTablePartitionDialog dialog = new NewTablePartitionDialog(UIUtils.getActiveWorkbenchShell(), monitor,
						parent);
				if (dialog.open() != IDialogConstants.OK_ID) {
					return null;
				}
				TablePartition newTablePartition = dialog.getTablePartition();
				if (parent.isPersisted()) {
					ArrayList<TablePartition> partList = (ArrayList<TablePartition>) parent.partitionCache
							.getCachedObjects();
					if (partList.size() != 0) {
						TablePartition model = partList.get(0);
						newTablePartition.setPartiType(model.getPartiType());
						newTablePartition.setPartiKey(model.getPartiKey());
					}
				}
				return newTablePartition;
			}
		}.execute();
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TablePartition, BaseTablePhysical>.ObjectCreateCommand command, Map<String, Object> options)
			throws DBException {
		// 新建表时在tablemanager中进行添加处理
		// 修改已存在表的分区时新增修改语句
		if (command.getObject().getParentObject().isPersisted() == true) {
			StringBuilder sql = new StringBuilder();

			sql.append("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			sql.append(" ADD PARTITION ");
			sql.append(command.getObject().getName());
			switch (command.getObject().getPartiType()) {
			case "LIST":
				sql.append(" VALUES('");
				sql.append(command.getObject().getPartiValue());
				sql.append("')");
				break;
			case "RANGE":
				sql.append(" VALUES LESS THAN(");
				sql.append(command.getObject().getPartiValue());
				sql.append(")");
				break;
			case "AUTOMATIC":
				sql.append(" VALUES LESS THAN(");
				sql.append(command.getObject().getPartiValue());
				sql.append(")");
				break;
			default:
				break;
			}

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add table partition sql: " + sql.toString());
			actions.add(new SQLDatabasePersistAction("Modify table, Add Partition", sql.toString()));
		} else {
			command.getObject().getParentObject().partitionCache.cacheObject(command.getObject());
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TablePartition, BaseTablePhysical>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		// 当表存在时才可进行删除action
		if (command.getObject().getParentObject().isPersisted() == true) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			sql.append(" DROP PARTITION ");
			sql.append(command.getObject().getName());

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop table partition sql: " + sql.toString());
			actions.add(new SQLDatabasePersistAction("Drop Partition", sql.toString()));
		}
		// 若是新增表情况时则直接将改对象从缓存中剔除
		else {
			command.getObject().getParentObject().partitionCache.removeObject(command.getObject(), false);
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList,
			SQLObjectEditor<TablePartition, BaseTablePhysical>.ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		// 当表存在时才可进行修改 action
		final String onlineKey = "online";
		if (command.getObject().getParentObject().isPersisted() == true && command.getProperty(onlineKey) != null) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			sql.append(" SET PARTITION ");
			sql.append("\"" + command.getObject().getName() + "\"");
			sql.append((boolean) command.getProperty("online") ? " ONLINE" : " OFFLINE");

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter table partition sql: " + sql.toString());
			actionList.add(new SQLDatabasePersistAction("Alter Partition", sql.toString()));
		}
	}

	public static class WarningDialog extends Dialog {
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

	static class NewTablePartitionDialog extends Dialog {
		final static String AUTOMATIC_TYPE = "AUTOMATIC";
		final static String HASH_TYPE = "HASH";

		private TablePartition partition;
		private BaseTablePhysical table;
		private DBRProgressMonitor monitor;
		private Text nameText;
		private Combo typeCombo;
		private Text valueText;
		private Combo colCombo;
		private Button addCol;
		private Button removeCol;
		private Text colText;

		// 自动扩展分区额外选项
		private Combo autoTypeCombo;
		private Text autoSpanText;

		public NewTablePartitionDialog(Shell parentShell, DBRProgressMonitor monitor, BaseTablePhysical table) {
			super(parentShell);
			this.table = table;
			this.monitor = monitor;
		}

		public TablePartition getTablePartition() {
			return this.partition;
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(500, 450);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			// 加载字段信息
			try {
				getShell().setText(Messages.dialog_tablePartition_create_title);
				Control container = super.createDialogArea(parent);
				Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
				composite.setLayoutData(new GridData(GridData.FILL_BOTH));

				nameText = UIUtils.createLabelText(composite, Messages.dialog_tablePartition_name, null);
				nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				typeCombo = UIUtils.createLabelCombo(composite, Messages.dialog_tablePartition_type, 8);
				typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				typeCombo.add("LIST");
				typeCombo.add("RANGE");
				typeCombo.add("HASH");
				typeCombo.add("AUTOMATIC");
				typeCombo.select(0);

				valueText = UIUtils.createLabelText(composite, Messages.dialog_tablePartition_value, null);
				valueText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				colCombo = UIUtils.createLabelCombo(composite, Messages.dialog_tablePartition_col_Combo_label, 8);
				colCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				Iterator<? extends DBSEntityAttribute> it = table.getAttributes(monitor).iterator();
				while (it.hasNext()) {
					String name = it.next().getName();
					colCombo.add(name);
				}
				colCombo.select(0);

				addCol = UIUtils.createPushButton(composite, Messages.dialog_tablePartition_add_col, null);
				addCol.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				removeCol = UIUtils.createPushButton(composite, Messages.dialog_tablePartition_remove_col, null);
				removeCol.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				colText = UIUtils.createLabelText(composite, Messages.dialog_tablePartition_col_Text_label, null);
				colText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				colText.setEnabled(false);

				addCol.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						// TODO Auto-generated method stub
						String text = colText.getText();
						String newCol = colCombo.getText();
						// 追加新的字段 前提是新字段名不存在于字段文本框内容中
						if (text != null && !"".equals(text)) {
							if (text.indexOf(newCol) == -1) {
								text += "," + colCombo.getText();
							}
						} else {
							text = colCombo.getText();
						}
						colText.setText(text);
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// TODO 小部件默认已选择事件
					}
				});
				removeCol.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						String text = colText.getText();
						String chooseColumn = colCombo.getText();
						String[] choseColumns = text.split(",");
						StringBuilder result = new StringBuilder();
						for (int i = 0; i < choseColumns.length; ++i) {
							if (!choseColumns[i].equals(chooseColumn)) {
								if (result.length() != 0) {
									result.append(",");
								}
								result.append(choseColumns[i]);
							} else {
								continue;
							}
						}
						colText.setText(result.toString());
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// TODO 小部件默认已选择事件
					}
				});

				autoTypeCombo = UIUtils.createLabelCombo(composite, Messages.dialog_tablePartition_col_AutoType_label,
						0);
				autoTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				autoTypeCombo.add("YEAR");
				autoTypeCombo.add("MONTH");
				autoTypeCombo.add("DAY");
				autoTypeCombo.add("HOUR");

				autoSpanText = UIUtils.createLabelText(composite, Messages.dialog_tablePartition_col_AutoSpan_label,
						null);
				autoSpanText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				// 默认禁用 autoTypeCombo 和 autoSpanText
				autoTypeCombo.setEnabled(false);
				autoSpanText.setEnabled(false);

				// 如果表已存在且分区也存在则对属性进行预设
				if (table.isPersisted()) {
					try {
						Collection<TablePartition> parts = table.getPartitions(monitor);
						if (parts != null) {
							if (parts.iterator().hasNext()) {
								TablePartition part = parts.iterator().next();
								String partType = part.getPartiType();
								String partKey = part.getPartiKey();
								typeCombo.setText(partType);
								partKey = partKey.replaceAll("\"", "");
								colText.setText(partKey);
								typeCombo.setEnabled(false);
								colCombo.setEnabled(false);
								colText.setEnabled(false);
								addCol.setEnabled(false);
								removeCol.setEnabled(false);
								if (AUTOMATIC_TYPE.equals(partType)) {
									autoTypeCombo.setText(part.getAutoPartiType());
									autoSpanText.setText(part.getAutoPartiSpan().toString());
								}
								autoTypeCombo.setEnabled(false);
								autoSpanText.setEnabled(false);
							}
						}
					} catch (DBException e) {
						e.printStackTrace();
					}
				}

				// 监听typeCombo，当属性为automatic时，运行设置自动扩展分区属性
				typeCombo.addSelectionListener(new SelectionListener() {
					// 根据选中类型做出动作
					@Override
					public void widgetSelected(SelectionEvent e) {
						String nowType = typeCombo.getText();
						if (HASH_TYPE.equals(nowType)) {
							nameText.setEnabled(false);
							valueText.setEnabled(true);
							addCol.setEnabled(true);
							removeCol.setEnabled(true);
							autoTypeCombo.setEnabled(false);
							autoSpanText.setEnabled(false);
						} else if (AUTOMATIC_TYPE.equals(nowType)) {
							nameText.setEnabled(true);
							valueText.setEnabled(true);
							addCol.setEnabled(false);
							removeCol.setEnabled(false);
							autoTypeCombo.setEnabled(true);
							autoSpanText.setEnabled(true);
						} else {
							nameText.setEnabled(true);
							valueText.setEnabled(true);
							addCol.setEnabled(true);
							removeCol.setEnabled(true);
							autoTypeCombo.setEnabled(false);
							autoSpanText.setEnabled(false);
						}
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
						// TODO 小部件默认已选择事件
					}
				});

				UIUtils.createInfoLabel(composite, Messages.dialog_tablePartition_create_info, GridData.FILL_HORIZONTAL,
						2);

				return parent;
			} catch (DBException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void okPressed() {
			String partitionName = nameText.getText();
			String partitionType = typeCombo.getText();
			String partitionValue = valueText.getText();
			String chooseColumn = colCombo.getText();
			String partitionKey = colText.getText();
			String autoPartitionType = autoTypeCombo.getText();
			String autoPartitionSpan = autoSpanText.getText();

			if (partitionType == null || partitionType.trim().isEmpty()) {
				new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition type cannot be empty").open();
				return;
			}
			if (partitionValue == null || partitionValue.trim().isEmpty()) {
				new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition value cannot be empty").open();
				return;
			}

			if (HASH_TYPE.equals(partitionType)) {
				if (partitionKey == null || partitionKey.trim().isEmpty()) {
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition key cannot be empty").open();
					return;
				}
			} else {
				if (partitionName == null || partitionName.trim().isEmpty()) {
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition name cannot be empty").open();
					return;
				}

				if (AUTOMATIC_TYPE.equals(partitionType)) {
					if (chooseColumn == null || chooseColumn.trim().isEmpty()) {
						new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition key cannot be empty").open();
						return;
					}
					if (autoPartitionType == null || autoPartitionType.trim().isEmpty()) {
						new WarningDialog(UIUtils.getActiveWorkbenchShell(),
								"Automatic partition interval type cannot be empty").open();
						return;
					}
					if (autoPartitionSpan == null || autoPartitionSpan.trim().isEmpty()) {
						new WarningDialog(UIUtils.getActiveWorkbenchShell(),
								"Automatic Partition interval span cannot be empty").open();
						return;
					}
				} else {
					if (partitionKey == null || partitionKey.trim().isEmpty()) {
						new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Partition key cannot be empty").open();
						return;
					}
				}
			}

			this.partition = new TablePartition(table, false, "");
			partition.setName(DBObjectNameCaseTransformer.transformObjectName(partition, partitionName));
			partition.setPartiType(DBObjectNameCaseTransformer.transformObjectName(partition, partitionType));
			partition.setPartiValue(DBObjectNameCaseTransformer.transformObjectName(partition, partitionValue));
			partition.setSubPartition(false);
			partition.setOnline(true);
			if (HASH_TYPE.equals(partitionType)) {
				partition.setName("AUTO_GENERATE");
				partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, colText.getText()));
			} else if (AUTOMATIC_TYPE.equals(partitionType)) {
				partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, chooseColumn));
				partition.setAutoPartiType(autoPartitionType);
				partition.setAutoPartiSpan(Integer.parseInt(autoPartitionSpan));
			} else {
				partition.setPartiKey(DBObjectNameCaseTransformer.transformObjectName(partition, colText.getText()));
			}
			super.okPressed();
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, TablePartition> getObjectsCache(TablePartition object) {
		return object.getParentObject().partitionCache;
	}
}
