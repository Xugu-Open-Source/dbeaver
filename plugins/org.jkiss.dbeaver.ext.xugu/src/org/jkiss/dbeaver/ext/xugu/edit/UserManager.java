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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBECommandFilter;
import org.jkiss.dbeaver.model.edit.DBECommandQueue;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户管理器，进行用户的创建，修改和删除，包含一个内部界面类，用于进行属性设定
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

    /**
	 * 新建用户界面显示前的准备工作
	 */
	@Override
	protected User createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) {
		DataSource parent = (DataSource) container;
		context.getUserParams();
		User newUser = new User(parent, monitor, false);
		
		// 修改已存在用户
		if (from instanceof User tplUser) {
            newUser.setName(tplUser.getName());
			// newUser.setPassword(tplUser.getPassword());
			newUser.setLocked(tplUser.isLocked());
			newUser.setExpired(tplUser.isExpired());
			newUser.setRoleList(tplUser.getRoleList());
			newUser.setPersisted(true);
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
            String key1 = CommonUtils.toString(command.getProperties().get(UserPropertyHandler.PASSWORD.toString()));
            String key2 = CommonUtils.toString(command.getProperties().get(UserPropertyHandler.PASSWORD_CONFIRM.toString()));
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
