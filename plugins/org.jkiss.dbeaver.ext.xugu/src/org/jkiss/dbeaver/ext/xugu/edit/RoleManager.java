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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * 角色管理器，进行索引的创建和删除，不支持修改，包含一个内部界面类，用于进行属性设定
 */
public class RoleManager extends SQLObjectEditor<Role, DataSource> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	public boolean canDeleteObject(Role object) {
		return true;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Role> getObjectsCache(Role object) {
		return object.getDataSource().roleCache;
	}

	@Override
	protected Role createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) {
		DataSource parent = (DataSource) container;
		Role newRole = new Role(parent, monitor, null);
		// 修改已存在用户
		if (from instanceof Role tplRole) {
            newRole.setName(tplRole.getName());
		}
		return newRole;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Role, DataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		// xfc 修改了创建模式的sql语句 暂时不支持设置数据库
		String user = command.getObject().getUserDesc();
		Role role = command.getObject();
		String sql = "CREATE ROLE " + role.getName();
		if (user != null && !user.equals("")) {
			sql += " INIT USER " + user;
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create role sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create role", sql));
		//新增角色向全部角色列表中添加
		User.getRoleNameList().add(role.getName());
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Role, DataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP ROLE " + DBUtils.getQuotedIdentifier(command.getObject());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop role sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop role", sql));
		//删除角色时，从全部角色列表删除
		List<String> roleString = User.roleNames; 
		for (int i =0 ; i<roleString.size();i++) {
			if(roleString.get(i).equals(command.getObject().getName())) {
				User.roleNames.remove(i);
			}
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Role, DataSource>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		// TODO 添加角色对象编辑动作
	}
}
