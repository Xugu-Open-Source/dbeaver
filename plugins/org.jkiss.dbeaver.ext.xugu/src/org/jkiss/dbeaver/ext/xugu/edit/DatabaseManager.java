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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Database;

import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.List;
import java.util.Map;

/**
 * 数据库管理器，进行数据库的增加和删除，不支持重命名， 包含一个内部界面类，用于进行属性设定
 */
public class DatabaseManager extends SQLObjectEditor<Database, DataSource> {
	Control container;

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Nullable
	@Override
	public DBSObjectCache<DataSource, Database> getObjectsCache(Database object) {
		return object.getDataSource().databaseCache;
	}

	@Override
	protected Database createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		DataSource parent = (DataSource) container;
		return new UITask<Database>() {
			@Override
			protected Database runTask() {
				NewDatabaseDialog dialog = new NewDatabaseDialog(UIUtils.getActiveWorkbenchShell(), parent);
				if (dialog.open() != IDialogConstants.OK_ID) {
					return null;
				}
				
				Database database = dialog.getDatabase();
				DBCSession session = context.getExecutionContext().openSession(monitor, DBCExecutionPurpose.USER , "CREATE DATABASE");

				String sql = "CREATE DATABASE " + database.getName();
				if (database.getCharset() != null) {
					sql += " CHARACTER SET '" + database.getCharset() + "'";
				}
				if (database.getTimeZone() != null) {
					sql += " TIME ZONE '" + database.getTimeZone() + "'";
				}

				try {
					session.prepareStatement(DBCStatementType.SCRIPT, sql, false, false, false).executeStatement();
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Create database " + database.getName() + "successfully").open();
				} catch (DBCException e) {
					log.error("Create database failed: " + sql, e);
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Create database failed： " + sql + "\n" + e.getMessage()).open();
				}
				return null;
			}
		}.execute();
	}

	/**
	 * 禁止删除数据库
	 */
	@Override
	public boolean canDeleteObject(Database object) {
		return false;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Database, DataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		Database database = command.getObject();
		String sql = "CREATE DATABASE " + database.getName();
		if (database.getCharset() != null) {
			sql += " CHARACTER SET '" + database.getCharset() + "'";
		}
		if (database.getTimeZone() != null) {
			sql += " TIME ZONE '" + database.getTimeZone() + "'";
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create database sql: " + sql.toString());
		actions.add(new SQLDatabasePersistAction("Create database", sql));

		try {
			DataSource sg = new DataSource(monitor, database.getDataSource().getContainer());
			sg.refreshObject(monitor);
		} catch (DBException e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Database, DataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		// TODO 添加数据库对象删除动作
	}

	static class NewDatabaseDialog extends Dialog {
		private Database database;
		private Text nameText;
		private Combo charsetCombo;
		private Combo isAddCombo;
		private Combo hourCombo;
		private Combo minuteCombo;

		public NewDatabaseDialog(Shell parentShell, DataSource dataSource) {
			super(parentShell);
			this.database = new Database(dataSource, "");
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(300, 350);
		}

		private Database getDatabase() {
			return database;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText(Messages.dialog_database_create_title);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 3);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			nameText = UIUtils.createLabelText(composite, Messages.dialog_database_name, null);
			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			charsetCombo = UIUtils.createLabelCombo(composite, Messages.dialog_database_charset, 8);
			for (int i = 0; i < Constants.DEFAULT_CHAR_SET.length; i++) {
				charsetCombo.add(Constants.DEFAULT_CHAR_SET[i]);
			}

			isAddCombo = UIUtils.createLabelCombo(composite, Messages.dialog_database_prefix, 8);
			isAddCombo.add("+");
			isAddCombo.add("-");

			hourCombo = UIUtils.createLabelCombo(composite, Messages.dialog_database_hour, 8);
			final int hourOfDay = 24;
			for (int i = 0; i < hourOfDay; i++) {
				if (i < 10) {
					hourCombo.add("0" + i);
				} else {
					hourCombo.add(i + "");
				}
			}

			minuteCombo = UIUtils.createLabelCombo(composite, Messages.dialog_database_minute, 8);
			final int minuteOfHour = 60;
			for (int i = 0; i < minuteOfHour; i++) {
				if (i < 10) {
					minuteCombo.add("0" + i);
				} else {
					minuteCombo.add(i + "");
				}
			}

			UIUtils.createInfoLabel(composite, Messages.dialog_database_create_info, GridData.FILL_HORIZONTAL, 3);

			return parent;
		}

		@Override
		protected void okPressed() {
			if (Utils.checkString(nameText.getText())) {
				database.setName(DBObjectNameCaseTransformer.transformObjectName(database, nameText.getText()));
				if (Utils.checkString(charsetCombo.getText())) {
					database.setCharset(
							DBObjectNameCaseTransformer.transformObjectName(database, charsetCombo.getText()));
				}
				if (Utils.checkString(isAddCombo.getText()) && Utils.checkString(hourCombo.getText())
						&& Utils.checkString(minuteCombo.getText())) {
					String timeZone = "GMT" + isAddCombo.getText() + hourCombo.getText() + ":" + minuteCombo.getText();
					database.setTimeZone(DBObjectNameCaseTransformer.transformObjectName(database, timeZone));
				}
				super.okPressed();
			} else {
				WarningDialog warn = new WarningDialog(UIUtils.getActiveShell(),
						Messages.dialog_database_warning_null_name);
				warn.open();
			}
		}
	}
}
