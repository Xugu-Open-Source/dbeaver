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
package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Synonym;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
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
import org.jkiss.utils.CommonUtils;

/**
 * 同义词管理器，进行同义词的创建和删除，不支持修改，包含一个内部界面类，用于进行属性设定
 */
public class SynonymManager extends SQLObjectEditor<Synonym, Schema> implements DBEObjectRenamer<Synonym> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("同义词名称不能为空");
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, Synonym> getObjectsCache(Synonym object) {
		return object.getSchema().synonymCache;
	}

//	@Override
//	protected Synonym createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
//			final Object container, Object from, Map<String, Object> options) throws DBException {
//		
//		if(container instanceof DataSource) {
//			DataSource dataSource = (DataSource) container;
//			return new UITask<Synonym>() {
//				@Override
//				protected Synonym runTask() {
//					NewSynonymDialog dialog = new NewSynonymDialog(UIUtils.getActiveWorkbenchShell(), dataSource);
//					if (dialog.open() != IDialogConstants.OK_ID) {
//						return null;
//					}
//					Synonym newSynonym = dialog.getSynonym();
//					return newSynonym;
//				}
//			}.execute();
//		}else {
//			Schema schema = (Schema) container;
//			return new UITask<Synonym>() {
//				@Override
//				protected Synonym runTask() {
//					NewSynonymDialog dialog = new NewSynonymDialog(UIUtils.getActiveWorkbenchShell(), schema);
//					if (dialog.open() != IDialogConstants.OK_ID) {
//						return null;
//					}
//					Synonym newSynonym = dialog.getSynonym();
//					return newSynonym;
//				}
//			}.execute();
//		}
//	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Synonym, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		Synonym synonym = command.getObject();
		String sql = "CREATE ";
		if (synonym.isPublic()) {
			sql += "PUBLIC ";
		}
		sql += "SYNONYM " + synonym.getParentObject().getName() + "." + synonym.getName() + " FOR "
				+ synonym.getParentObject().getName() + "." + synonym.getTargetName();

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create synonym sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create synonym", sql));
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Synonym, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		Synonym synonym = command.getObject();
		String sql = "DROP ";
		if (synonym.isPublic()) {
			sql += "PUBLIC ";
		}
		sql += "SYNONYM " + synonym.getParentObject().getName() + "." + DBUtils.getQuotedIdentifier(synonym);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop synonym sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop synonym", sql));
	}

	@Override
	public void renameObject(DBECommandContext commandContext, Synonym object, Map<String, Object> options,
			String newName) throws DBException {
		throw new DBException("Direct synonym rename is not yet implemented in " + OemConfig.OEM_NAME_EN
				+ " Database. You should use export/import functions for that.");
	}

	static class NewSynonymDialog extends Dialog {
		private Synonym synonym;
		private Text nameText;
		private Text tarNameText;
		private Button isPublicButton;

		public NewSynonymDialog(Shell parentShell, Schema dataSource) {
			super(parentShell);
			this.synonym = new Synonym(dataSource, null);
		}
		
//		public NewSynonymDialog(Shell parentShell, DataSource dataSource) {
//			super(parentShell);
//			this.synonym = new Synonym(dataSource, null);
//		}
		

		public Synonym getSynonym() {
			return synonym;
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
//			getShell().setText(Messages.dialog_synonym_create_title);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			nameText = UIUtils.createLabelText(composite, Messages.dialog_synonym_name, null);
			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			tarNameText = UIUtils.createLabelText(composite, Messages.dialog_synonym_target, null);
			tarNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			isPublicButton = UIUtils.createCheckbox(composite, "Is Public", false);

			UIUtils.createInfoLabel(composite, Messages.dialog_synonym_create_info, GridData.FILL_HORIZONTAL, 2);

			return parent;
		}

		@Override
		protected void okPressed() {
			if (Utils.checkString(nameText.getText())) {
				synonym.setName(DBObjectNameCaseTransformer.transformObjectName(synonym, nameText.getText()));
				synonym.setTargetName(DBObjectNameCaseTransformer.transformObjectName(synonym, tarNameText.getText()));
				synonym.setPublic(isPublicButton.getSelection());
				super.okPressed();
			} else {
				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "同义词名称不能为空");
				warnDialog.open();
			}
		}
	}

	@Override
	protected Synonym createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
}
