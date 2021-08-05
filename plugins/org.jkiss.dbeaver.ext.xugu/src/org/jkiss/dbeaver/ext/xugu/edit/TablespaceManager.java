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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Tablespace;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * 表空间管理器，进行表空间的创建，修改和删除，包含一个内部界面类，用于进行属性设定
 * 
 * @author Xugu
 */
public class TablespaceManager extends SQLObjectEditor<Tablespace, DataSource> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	public boolean canCreateObject(Object container) {
		return false;
	}

	@Override
	public boolean canEditObject(Tablespace object) {
		return false;
	}

	@Override
	public boolean canDeleteObject(Tablespace object) {
		return false;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, Tablespace> getObjectsCache(Tablespace object) {
		return object.getDataSource().getTablespaceCache();
	}

	@Override
	protected Tablespace createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		DataSource parent = (DataSource) container;
		return new UITask<Tablespace>() {
			@Override
			protected Tablespace runTask() {
				NewTablespaceDialog dialog = new NewTablespaceDialog(UIUtils.getActiveWorkbenchShell(), parent);
				if (dialog.open() != IDialogConstants.OK_ID) {
					return null;
				}
				Tablespace newTablespace = dialog.getTableSpace();
				return newTablespace;
			}
		}.execute();
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Tablespace, DataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		// xfc 修改了创建模式的sql语句 暂时不支持设置数据库
		Tablespace tablespace = command.getObject();
		String sql = "CREATE TABLESPACE " + tablespace.getName();
		if (command.getObject().getNodeId() > 0) {
			sql += " ON NODE " + command.getObject().getNodeId();
		} else {
			sql += " ON ALL NODE";
		}
		sql += " DATAFILE '" + tablespace.getFilePath() + "'";

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add tablespace sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create Tablespace", sql));
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Tablespace, DataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP TABLESPACE " + DBUtils.getQuotedIdentifier(command.getObject());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop tablespace sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop Tablespace", sql));
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Tablespace, DataSource>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperties().size() > 1 || command.getProperty(commentKey) == null) {
			StringBuilder query = new StringBuilder("ALTER TABLESPACE ");
			query.append(command.getObject().getName()).append(" ");
			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter tablespace sql: " + query.toString());
			actionList.add(new SQLDatabasePersistAction(query.toString()));
		}
	}

	static class NewTablespaceDialog extends Dialog {
		private Tablespace space;
		private Text nameText;
		private Text fileText;
		private Text nodeText;

		public NewTablespaceDialog(Shell parentShell, DataSource dataSource) {
			super(parentShell);
			try {
				this.space = new Tablespace(dataSource, null);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		public Tablespace getTableSpace() {
			return space;
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
			getShell().setText(Messages.dialog_tablespace_create_title);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			nameText = UIUtils.createLabelText(composite, Messages.dialog_tablespace_name, null);
			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			nodeText = UIUtils.createLabelText(composite, Messages.dialog_tablespace_nodeID, null);
			nodeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			fileText = UIUtils.createLabelText(composite, Messages.dialog_tablespace_filePath, null);
			fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			UIUtils.createInfoLabel(composite, Messages.dialog_tablespace_create_info, GridData.FILL_HORIZONTAL, 2);

			return parent;
		}

		@Override
		protected void okPressed() {
			if (Utils.checkString(nameText.getText())) {
				space.setName(DBObjectNameCaseTransformer.transformObjectName(space, nameText.getText()));
				space.setFilePath(DBObjectNameCaseTransformer.transformObjectName(space, fileText.getText()));
				if (nodeText.getText() != null) {
					space.setNodeId(Integer
							.parseInt(DBObjectNameCaseTransformer.transformObjectName(space, nodeText.getText())));
				} else {
					space.setNodeId(0);
				}
				super.okPressed();
			} else {
				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Table space name can't be empty");
				warnDialog.open();
			}
		}
	}
}
