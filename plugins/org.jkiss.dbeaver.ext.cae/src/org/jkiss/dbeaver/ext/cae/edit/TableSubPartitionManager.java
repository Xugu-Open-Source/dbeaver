/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.cae.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cae.Messages;
import org.jkiss.dbeaver.ext.cae.Utils;
import org.jkiss.dbeaver.ext.cae.config.OemConfig;
import org.jkiss.dbeaver.ext.cae.model.TableColumn;
import org.jkiss.dbeaver.ext.cae.model.BaseTablePhysical;
import org.jkiss.dbeaver.ext.cae.model.TableSubPartition;
import org.jkiss.dbeaver.ext.cae.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
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
 * 二级分区管理器，进行二级分区的创建和删除，修改仅支持设置是否在线，包含一个内部界面类，用于进行属性设定
 */
public class TableSubPartitionManager extends SQLObjectEditor<TableSubPartition, BaseTablePhysical> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	protected TableSubPartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		BaseTablePhysical parent = (BaseTablePhysical) container;
		// 仅允许对新创建的表进行添加二级分区操作
		if (parent.isPersisted() == false) {
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
						if (partList.size() != 0) {
							TableSubPartition model = partList.get(0);
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

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TableSubPartition, BaseTablePhysical>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		// 表存在时，禁用二级分区操作
		if (command.getObject().getParentObject().isPersisted() == true) {
			new UITask<String>() {
				@Override
				protected String runTask() {
					WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Can't create sub partition on existed table");
					if (dialog2.open() != IDialogConstants.OK_ID) {
						return null;
					}
					return null;
				}
			}.execute();
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TableSubPartition, BaseTablePhysical>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		// 不允许对二级分区进行删除操作
		if (command.getObject().getParentObject().isPersisted() == true) {
			new UITask<String>() {
				@Override
				protected String runTask() {
					WarningDialog dialog2 = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Can't delete sub partition on existed table");
					if (dialog2.open() != IDialogConstants.OK_ID) {
						return null;
					}
					return null;
				}
			}.execute();
		}
		// 若是新增表情况时则直接将改对象从缓存中剔除
		else {
			command.getObject().getParentObject().subPartitionCache.removeObject(command.getObject(), true);
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList,
			SQLObjectEditor<TableSubPartition, BaseTablePhysical>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		// 当表存在时才可进行修改 action
		final String onlineKey = "online";
		if (command.getObject().getParentObject().isPersisted() == true && command.getProperty(onlineKey) != null) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getName());
			sql.append(" SET SUBPARTITION ");
			sql.append(command.getObject().getName());
			sql.append((boolean) command.getProperty("online") ? " ONLINE" : " OFFLINE");

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add subpartition sql: " + sql.toString());
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
				if(cols != null) {
					Iterator<? extends DBSEntityAttribute> it = cols.iterator();
					while (it.hasNext()) {
						String name = it.next().getName();
						colCombo.add(name);
					}
				}
			} catch (DBException e) {
				e.printStackTrace();
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
				org.jkiss.dbeaver.ext.cae.views.WarningDialog warnDialog = new org.jkiss.dbeaver.ext.cae.views.WarningDialog(
						UIUtils.getActiveWorkbenchShell(), "Table Subpartition name cannot be null");
				warnDialog.open();
			}
		}

	}

	@Override
	public DBSObjectCache<? extends DBSObject, TableSubPartition> getObjectsCache(TableSubPartition object) {
		return object.getParentObject().subPartitionCache;
	}
}
