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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Database;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import cn.hutool.log.StaticLog;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 模式管理器，进行模式的创建和删除，修改（重命名或添加注释信息）包含一个内部界面类，用于进行属性设定
 */
public class SchemaManager extends SQLObjectEditor<Schema, DataSource> implements DBEObjectRenamer<Schema> {
	
	
	
	 private  Object currentContainer = null;
	 private  String userName = null;
	
	
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Schema name can not be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Schema> getObjectsCache(Schema object) {
		return object.getDataSource().schemaCache;
	}

	@Override
	protected Schema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) {
//		DataSource parent = ((Database) container).getDataSource();
		
		currentContainer = container;
		DataSource parent = (DataSource) container ;
		return new UITask<Schema>() {
			@Override
			protected Schema runTask() {
				NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
				if (dialog.open() != IDialogConstants.OK_ID) {
					return null;
				}
				Schema newSchema = new Schema(parent, -1, dialog.getSchema().getName());
//				newSchema.setUser(dialog.getUser());
				return newSchema;
			}
		}.execute();		
//		return new Schema((DataSource)container,0,"NewSchema");
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Schema, DataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		// 修改了创建模式的sql语句，暂时不支持设置数据库
//		User user = command.getObject().getUser();
		userName =  NewUserDialog.userNameString;
		User user = new User((DataSource)currentContainer, userName, false);
		Schema schema = command.getObject();
		StringBuilder desc = new StringBuilder();

		if (!CommonUtils.isEmpty(schema.getName())) {
			desc.append("CREATE SCHEMA ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), schema.getName()));
			if (!CommonUtils.isEmpty(user.getName())) {
				desc.append(" AUTHORIZATION ");
				desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), user.getName()));
			}
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create schema sql: " + desc.toString());
		actions.add(new SQLDatabasePersistAction("create schema", desc.toString()));
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Schema, DataSource>.ObjectRenameCommand command,
			Map<String, Object> options) {
		StringBuilder desc = new StringBuilder(100);
		if (!CommonUtils.isEmpty(command.getNewName())) {
			desc.append("ALTER SCHEMA ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()));
			desc.append(" RENAME TO ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(),
					command.getNewName().toUpperCase()));
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct rename schema sql: " + desc.toString());
		actionList.add(new SQLDatabasePersistAction("rename schema", desc.toString()));
	}

	/**
	 * 对模式的修改只能是添加注释信息
	 */
	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Schema, DataSource>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		String comment = buildComment(command.getObject());
		if (comment != null) {
			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter schema comment sql: " + comment);
			actionList.add(new SQLDatabasePersistAction("Comment on Schema", comment));
		}
		
		String owner = buildOwner(command.getObject());
		if (owner != null) {
			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter schema owner sql: " + owner);
			actionList.add(new SQLDatabasePersistAction("Owner on Schema", owner));
		}
	}

	@Override
	public void renameObject(DBECommandContext commandContext, Schema object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
		// 在执行完重命名后，修改对象名称（用于前台数据刷新）
		object.setName(newName);
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Schema, DataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP SCHEMA "
				+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getObject().getName());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop schema sql: " + sql);
		actions.add(new SQLDatabasePersistAction("drop schema", sql));
	}

	static class NewUserDialog extends Dialog {

		private Schema schema;
		private User user;
		private Text nameText;
		private Text passwordText;
		private DataSource dataSource;
		private Combo schemaOwner;
		private DBRProgressMonitor monitor;
		private static String userNameString ;

		public NewUserDialog(Shell parentShell, DataSource dataSource, DBRProgressMonitor monitor) {
			super(parentShell);
			this.schema = new Schema(dataSource, -1, null);
//			this.user = new User(dataSource, null, monitor);
			this.dataSource = dataSource;
			this.monitor = monitor;
		}
		
		

		public User getUser() {
			return user;
		}

		public Schema getSchema() {
			return schema;
		}

		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Point getInitialSize() {
			return new Point(300, 200);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText(Messages.dialog_schema_create_title);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 3);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			nameText = UIUtils.createLabelText(composite, Messages.dialog_schema_name, null);
			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			schemaOwner = UIUtils.createLabelCombo(composite, Messages.dialog_schema_user, 8);
			schemaOwner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			try {
				Collection<User> userList = this.dataSource.userCache.getAllObjects(monitor, this.dataSource);
				Iterator<User> it = userList.iterator();
				while (it.hasNext()) {
					User user = it.next();
					schemaOwner.add(user.getName());
				}
			} catch (DBException e) {
				e.printStackTrace();
			}

			UIUtils.createInfoLabel(composite, Messages.dialog_schema_create_info, GridData.FILL_HORIZONTAL, 4);

			return parent;
		}

		@Override
		protected void okPressed() {
			if (Utils.checkString(nameText.getText())) {
//				user = new User(null,DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
//				user.setName(DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
				userNameString = DBObjectNameCaseTransformer.transformObjectName(schema, schemaOwner.getText());
				schema.setName(DBObjectNameCaseTransformer.transformObjectName(schema, nameText.getText()));
//				schema.setUser(user);
				super.okPressed();
			} else {
				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Schema name can not be empty");
				warnDialog.open();
			}
		}

	}

	private String buildComment(Schema schema) {
		if (!CommonUtils.isEmpty(schema.getComment())) {
			return "COMMENT ON SCHEMA " + schema.getName() + " IS " + SQLUtils.quoteString(schema, schema.getComment());
		}
		return null;
	}
	
	private String buildOwner(Schema schema) {
		if (!CommonUtils.isEmpty(schema.getOwner())) {
			return "ALTER SCHEMA \"" + schema.getName() + "\" OWNER TO \"" + schema.getOwner() + "\"";
		}
		return null;
	}
}
