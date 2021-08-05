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

import org.eclipse.core.runtime.ISafeRunnable;
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
import org.jkiss.dbeaver.ext.xugu.edit.SchemaManager.NewUserDialog;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Package;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.edit.*;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Date;
import java.time.DateTimeException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户管理器，进行用户的创建，修改和删除，包含一个内部界面类，用于进行属性设定
 * 
 * @author Xugu
 */
public class UserManager extends SQLObjectEditor<User, DataSource>
		implements DBEObjectMaker<User, DataSource>, DBECommandFilter<User> {
	
	/**
	 * 系统角色信息
	 */
	public static Collection<Role> roleList;
	
	
	public static Collection<Role> getRolesList() {
		return roleList;
	}
	
	
	
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, User> getObjectsCache(User object) {
		return object.getDataSource().userCache;
	}

	@Override
	public boolean canDeleteObject(User object) {
		return true;
	}
	
	
	static class CreateUserDialog extends Dialog{
		private DataSource dataSource;
		private Combo roleCombo;
		private Text userNameText;
		private Text passwordText;
		private Text roleText;
		private Text isLockedText;
		private Text expiredText;
		private Text validUntilText;
		
		
		private Role role;
		private User user;
		private String roleName;
		private String userName;
		private String password;
		private String isLocked;
		private String expired;
		private String validUntil;

			
		private DBRProgressMonitor monitor;
		
		public CreateUserDialog(Shell shell,DataSource dataSource,DBRProgressMonitor monitor) {
			super(shell);
			this.user = new User(dataSource,monitor);
			this.dataSource = dataSource;
			this.monitor = monitor;
		}
		
		public Role getRole() {
			return role;
		}
		
	
		
		public User user() {
			return user;
		}
		
		public String getUserName() {
			return userName;
		}
		
		public String getPassword() {
			return password;
		}
		
		public String getRoleName() {
			return roleName;
		}
		
		public String getIsLocked() {
			return isLocked;
		}
		
		public String getExpired() {
			return expired;
		}
		
		public String getValidUntil() {
			return validUntil;
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
			getShell().setText(Messages.dialog_user_create_title);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 3);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			userNameText = UIUtils.createLabelText(composite, Messages.dialog_connection_user_name, null);
			userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

//			passwordText = UIUtils.createLabelText(composite, Messages.dialog_connection_password, null);
//			passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			
//			isLockedText = UIUtils.createLabelText(composite, Messages.dialog_connection_islocked, null);
//			isLockedText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			
//			expiredText = UIUtils.createLabelText(composite, Messages.dialog_connection_expired, null);
//			expiredText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			
//			validUntilText = UIUtils.createLabelText(composite, Messages.dialog_connection_valid_until, null);
//			validUntilText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			
//			roleCombo = UIUtils.createLabelCombo(composite, Messages.dialog_connection_role, 8);
//			roleCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
			try {
				roleList = this.dataSource.roleCache.getAllObjects(monitor, this.dataSource);
//				Iterator<Role> it = roleList.iterator();
//				while(it.hasNext()) {
//					Role role = it.next();
//					roleCombo.add(role.getName());
//				}
			} catch (DBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			UIUtils.createInfoLabel(composite, Messages.dialog_user_create_info, GridData.FILL_HORIZONTAL, 4);
			return parent;
		}

		@Override
		protected void okPressed() {
			if (Utils.checkString(userNameText.getText())) {
//				user = new User(null,DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
//				user.setName(DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
//				userNameString = DBObjectNameCaseTransformer.transformObjectName(schema, schemaOwner.getText());
//				schema.setName(DBObjectNameCaseTransformer.transformObjectName(schema, nameText.getText()));
//				schema.setUser(user);
				userName = DBObjectNameCaseTransformer.transformName(dataSource, userNameText.getText());
//				password = DBObjectNameCaseTransformer.transformName(dataSource, passwordText.getText());
//				isLocked = DBObjectNameCaseTransformer.transformName(dataSource, isLockedText.getText());
//				validUntil = DBObjectNameCaseTransformer.transformName(dataSource, validUntilText.getText());
//				expired = DBObjectNameCaseTransformer.transformName(dataSource, expiredText.getText());
//				roleName = DBObjectNameCaseTransformer.transformName(dataSource, roleCombo.getText());
				user.setName(userName);
//				user.setPassword(password);
//				user.setLocked(Boolean.getBoolean(isLocked));
//				user.setUntilTime(validUntil);
//				user.setExpired(Boolean.getBoolean(expired));
//				user.setRoleList(roleName);
				super.okPressed();
			} else {
				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "user name can not be empty");
				warnDialog.open();
			}
		}
	}

	/**
	 * 新建用户界面显示前的准备工作
	 */
	@Override
	protected User createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) {
		DataSource parent = (DataSource) container;
		context.getUserParams();
		User newUser = new User(parent, monitor);
		
		// 修改已存在用户
		if (from instanceof User) {
			User tplUser = (User) from;
			newUser.setName(tplUser.getName());
			newUser.setPassword(tplUser.getPassword());
			newUser.setLocked(tplUser.isLocked());
			newUser.setExpired(tplUser.isExpired());
		}
		// 创建新用户
		else {
			return new UITask<User>() {
				@Override
				protected User runTask() {
					CreateUserDialog dialog = new CreateUserDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
					if (dialog.open() != IDialogConstants.OK_ID) {
						return null;
					}
					User newUser = new User(parent,dialog.getUserName());
					return newUser;
				}
			}.execute();			
		}
		return newUser;
	}

	/**
	 * 点击确定后，真正执行新建用户操作
	 */
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<User, DataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		User user = command.getObject();
		if (command.getProperties() != null) {
//			String name = command.getProperties().get(UserPropertyHandler.NAME.toString()).toString();
			String key1 = command.getProperties().get(UserPropertyHandler.PASSWORD.toString()).toString();
			String key2 = command.getProperties().get(UserPropertyHandler.PASSWORD_CONFIRM.toString()).toString();
			String untilTimeString = null;
			if(command.getProperties().get(UserPropertyHandler.UNTIL_TIME.toString())!=null) {
			 untilTimeString =  command.getProperties().get(UserPropertyHandler.UNTIL_TIME.toString()).toString();
			}
//			Boolean isLockBoolean = Boolean.valueOf(command.getProperties().get(UserPropertyHandler.LOCKED.toString()).toString());
			 Object roleList = "";
			if (command.getProperties().get(UserPropertyHandler.ROLE_LIST.toString()) != null) {
				roleList = command.getProperties().get(UserPropertyHandler.ROLE_LIST.toString());
			}
			String roleString = "";
			String [] roleStrings=null;
			if(roleList!="") {
				  roleStrings = (String[])roleList;	
			}
			if(roleStrings!=null) {
				for (int i = 0; i < roleStrings.length; i++) {
					if(roleStrings.length==1) {
						  roleString = roleStrings[i];
					}else {
						if(i==roleStrings.length-1) {
							roleString += roleStrings[i];
						}else {
							roleString += roleStrings[i]+",";
						}
					}
				}
			}
			if (!Objects.equals(key1, key2)) {
				 throw new DBException("确认密码错误，请重新输入！");
			} else {
				user.setPassword(key1);
				user.setRoleList(roleString);
				if(untilTimeString!=null&&untilTimeString!="") {
					user.setUntilTime(untilTimeString);
				}
				user.setPersisted(true);
				StringBuilder sql = new StringBuilder();
				sql.append("CREATE USER ");
				sql.append(user.getName());
				sql.append("\nIDENTIFIED BY '");
				sql.append(user.getPassword());
				sql.append("'");
				if (user.getRoleList() != null && !"".equals(user.getRoleList())) {
					sql.append(" DEFAULT ROLE ");
					String[] roles = user.getRoleList().split(",");
					for (int i = 0; i < roles.length; i++) {
						sql.append(roles[i]);
						if (i != roles.length - 1) {
							sql.append(",");
						}
					}
				}
				if(untilTimeString!=null) {
					sql.append(" \nVALID UNTIL ");
					sql.append("'"+user.getUntilTime()+"'");
				}
				sql.append(user.isLocked() ? " ACCOUNT LOCK" : "");
				sql.append(user.isExpired() ? " PASSWORD EXPIRED" : "");

				log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create user sql: " + sql.toString());
				DBEPersistAction action = new SQLDatabasePersistAction("Create User", sql.toString());
				actions.add(action);
			}
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<User, DataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP USER " + command.getObject().getName();

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop user sql: " + sql);
		DBEPersistAction action = new SQLDatabasePersistAction("Drop User", sql);
		actions.add(action);
		String userNameString = command.getObject().getName();
		//删除用户后从用户集合中移除该用户。
		List<User> users = User.users;
		for (int i = 0; i < users.size(); i++) {
			if(users.get(i).getName().equals(userNameString));
			User.users.remove(i);
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<User, DataSource>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		String key1 = command.getProperties().get(UserPropertyHandler.PASSWORD.toString()).toString();
		String key2 = command.getProperties().get(UserPropertyHandler.PASSWORD_CONFIRM.toString()).toString();
		if (!Objects.equals(key1, key2)) {
			 throw new DBException("确认密码错误，请重新输入！");
		}
		
		
		for (String k : options.keySet()) {
			log.debug(options.get(k));
		}
		String sql = "ALTER USER " + command.getObject().getName() + " IDENTIFIED BY ";

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter user sql: " + sql);
		DBEPersistAction action = new SQLDatabasePersistAction("Alter User", sql);
		actionList.add(action);
	}

	@Override
	public void filterCommands(DBECommandQueue<User> queue) {
		// TODO 过滤命令
	}
}
