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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.RoleAuthority;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * 角色属性修改逻辑，根据界面上设置的用户相关属性生成指定数据库操作 action
 */
public class CommandChangeRole extends DBECommandComposite<Role, RolePropertyHandler> {
	protected CommandChangeRole(Role role) {
		super(role, "Alter Role");
	}

	@Override
	public void updateModel() {
		for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
			switch (RolePropertyHandler.valueOf((String) entry.getKey())) {
			case NAME:
				getObject().setName(CommonUtils.toString(entry.getValue()));
				break;
			default:
				break;
			}
		}
	}

	@Override
	public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			Map<String, Object> options) {
		List<DBEPersistAction> actions = new ArrayList<>();
		boolean newRole = !getObject().isPersisted();
		// 创建新角色
		if (newRole) {
			String user = getObject().getUserDesc();
			Role role = getObject();
			String sql = "CREATE ROLE " + role.getName();
			if (user != null && !user.equals("")) {
				sql += " INIT USER " + user;
			}
			actions.add(new SQLDatabasePersistAction("Create role", sql));
		}
		// 修改角色权限
		else {
			// 对权限做额外处理
			// 库级权限
			Collection<RoleAuthority> databaseAuthorities = getObject().getRoleDatabaseAuthorities();
			// 对象级权限
			Collection<RoleAuthority> objectAuthorities = getObject().getRoleObjectAuthorities();
			// 二级对象权限
			Collection<RoleAuthority> subObjectAuthorities = getObject().getRoleSubObjectAuthorities();
			Iterator<RoleAuthority> it = null;
			if(subObjectAuthorities!=null) {
				it = subObjectAuthorities.iterator();
			}

			while (it.hasNext()) {
				RoleAuthority authority = it.next();
				if (!(authority.getName().contains("列") || authority.getName().contains("触发器"))) {
					subObjectAuthorities.remove(authority);
				}
			}
			String schema = "";
			String object = "";
			String realTargetName = "";
			String[] newAuthorities = null;
			RoleAuthority authority = null;
			for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
				switch (RolePropertyHandler.valueOf((String) entry.getKey())) {
				case DATABASE_AUTHORITY:
					if(databaseAuthorities==null) {
						break;
					}
					// 遍历新权限列表，若旧权限不存在于其中，则做revoke操作
					it = databaseAuthorities.iterator();
					newAuthorities = (String[]) entry.getValue();
					while (it.hasNext()) {
						authority = it.next();
						boolean inListFlag = false;
						for (int i = 0, l = newAuthorities.length; i < l; i++) {
							if (authority.getName().equals(newAuthorities[i])) {
								inListFlag = true;
								break;
							}
						}
						// 旧权限不在列表中则revoke
						if (!inListFlag && authority != null) {
							actions.add(new SQLDatabasePersistAction("Revoke role",
									"REVOKE " + Utils.transformAuthority(authority.getName(), true) + " FROM "
											+ getObject().getName()));
						}
					}
					// 遍历旧权限列表，若新权限不存在于其中，则做grant操作
					it = databaseAuthorities.iterator();
					for (int i = 0, l = newAuthorities.length; i < l; i++) {
						boolean inListFlag = false;
						while (it.hasNext()) {
							authority = it.next();
							if (authority.getName().equals(newAuthorities[i])) {
								inListFlag = true;
								break;
							}
						}
						// 新权限不在列表中则grant
						if (!inListFlag) {
							actions.add(new SQLDatabasePersistAction("Grant user",
									"GRANT " + Utils.transformAuthority(newAuthorities[i], true) + " TO "
											+ getObject().getName()));
						}
					}
					break;
				case OBJECT_AUTHORITY:
					if(objectAuthorities==null) {
						break;
					}
					it = objectAuthorities.iterator();
					newAuthorities = (String[]) entry.getValue();
					schema = getProperties().get("TARGET_SCHEMA").toString();
					object = getProperties().get("TARGET_OBJECT").toString();
					realTargetName = "\"" + schema + "\".\"" + object + "\"";
					// 遍历新权限列表，若旧权限不存在于其中，则做revoke操作
					while (it.hasNext()) {
						authority = it.next();
						boolean inListFlag = false;
						for (int i = 0, l = newAuthorities.length; i < l; i++) {
//							if (authority.getName().contains(newAuthorities[i])
//									&& authority.getTargetName().equals(realTargetName)) {
//								inListFlag = true;
//								break;
//							}
							if(!newAuthorities[i].contains("\"")) {
								newAuthorities[i] = newAuthorities[i]+":"+realTargetName;
							}
							if (authority.getName().contains(newAuthorities[i])) {
//									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}
						// 旧权限不在列表中则revoke
						if (!inListFlag && authority != null) {
							actions.add(new SQLDatabasePersistAction("Revoke role",
									"REVOKE " + Utils.transformAuthority(authority.getName(), false) + " " + "\""
											+ schema + "\".\"" + object + "\"" + " FROM " + getObject().getName()));
						}
					}
					// 遍历旧权限列表，若新权限不存在于其中，则做grant操作
					it = objectAuthorities.iterator();
					for (int i = 0, l = newAuthorities.length; i < l; i++) {
						boolean inListFlag = false;
						while (it.hasNext()) {
							authority = it.next();
							if (authority.getName().contains(newAuthorities[i])){
//									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}
						// 新权限不在列表中则grant
						if (!inListFlag) {
							actions.add(new SQLDatabasePersistAction("Grant role",
									"GRANT " + Utils.transformAuthority(newAuthorities[i], false) + " " + realTargetName
											+ " TO " + getObject().getName()));
						}
					}
					break;
				case SUB_OBJECT_AUTHORITY:
					if(subObjectAuthorities==null) {
						break;
					}
					it = subObjectAuthorities.iterator();
					newAuthorities = (String[]) entry.getValue();
					schema = getProperties().get("TARGET_SCHEMA").toString();
					object = getProperties().get("TARGET_OBJECT").toString();
					String subObject = getProperties().get("SUB_TARGET_OBJECT").toString();
					String subObjectType = getProperties().get("SUB_TARGET_TYPE").toString();
					realTargetName = "\"" + schema + "\".\"" + object + "\"" + ".\"" + subObject + "\"";
						
					ArrayList<String> arrayList = new ArrayList();
				
					// 遍历新权限列表，若旧权限不存在于其中，则做revoke操作
					while (it.hasNext()) {
						authority = it.next();
						boolean inListFlag = false;
						int count = 0;
						for (int i = 0, l = newAuthorities.length; i < l; i++) {
							count ++;
							if (authority.getName().contains(newAuthorities[i])) {
//									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}   
					 
//						if(inListFlag||count == newAuthorities.length) {
//							continue;
//						}
						// 旧权限不在列表中则revoke
						if (!inListFlag && authority != null) {
							if (!"COLUMN".equals(subObjectType)) {
								actions.add(new SQLDatabasePersistAction("Revoke role",
										"REVOKE " + Utils.transformAuthority(authority.getName(), false) + " " + "\""
												+ schema + "\".\"" + object + "\"" + " FROM " + getObject().getName()));
								arrayList.add( Utils.transformAuthority(authority.getName(), false)+object);
							}
							// 对列对象做特殊处理
							else {
								actions.add(new SQLDatabasePersistAction("Revoke role",
										"REVOKE " + Utils.transformColumnAuthority(authority.getName()) + "("
												+ subObject + ") ON " + "\"" + schema + "\".\"" + object + "\""
												+ " FROM " + getObject().getName()));
								arrayList.add( Utils.transformColumnAuthority(authority.getName())+object);
							}
						}
					}
					// 遍历旧权限列表，若新权限不存在于其中，则做grant操作
					it = subObjectAuthorities.iterator();
					int length = subObjectAuthorities.size();
					for (int i = 0, l = newAuthorities.length; i < l; i++) {
						int count = 0;
						boolean inListFlag = false;
						while (it.hasNext()) {
							authority = it.next();
							count++;
							if (authority.getName().contains(newAuthorities[i])) {
//									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}	
						}
//						if(inListFlag||count == length) {
//							continue;
//						}
						// 新权限不在列表中则grant
						if (!inListFlag) {
							if (!"COLUMN".equals(subObjectType)) {
								actions.add(new SQLDatabasePersistAction("Grant role",
										"GRANT " + Utils.transformAuthority(newAuthorities[i], false) + " "
												+ realTargetName + " TO " + getObject().getName()));
								arrayList.add( Utils.transformAuthority(authority.getName(), false)+realTargetName);
							}
							// 对列类型做特殊处理
							else {
								actions.add(new SQLDatabasePersistAction("Grant role",
										"GRANT " + Utils.transformColumnAuthority(newAuthorities[i]) + "(" + subObject
												+ ") ON " + "\"" + schema + "\".\"" + object + "\"" + " TO "
												+ getObject().getName()));
		//						arrayList.add( Utils.transformColumnAuthority(authority.getName())+object);
							}
						}
					}
//					for(int i = 0 ; i<arrayList.size();i++) {
//						System.out.println(arrayList.get(i));
//					}
					break;
				default:
					break;
				}
			}
		}
		return actions.toArray(new DBEPersistAction[actions.size()]);
	}
}
