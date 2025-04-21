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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.model.UserAuthority;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户属性修改逻辑，根据界面上设置的用户相关属性生成指定数据库操作 action
 */
public class CommandChangeUser extends DBECommandComposite<User, UserPropertyHandler> {
	protected CommandChangeUser(User user) {
		super(user, Messages.edit_command_change_user_name);
	}

	@Override
	public void updateModel() {
		for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
			switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
			case NAME:
				getObject().setName(CommonUtils.toString(entry.getValue()));
				break;
			case PASSWORD:
				getObject().setPassword((CommonUtils.toString(entry.getValue())));
				break;
			case LOCKED:
				getObject().setLocked(CommonUtils.toBoolean(entry.getValue()));
				break;
			case EXPIRED:
				getObject().setExpired(CommonUtils.toBoolean(entry.getValue()));
				break;
			case UNTIL_TIME:
				getObject().setUntilTime(CommonUtils.toString(entry.getValue()));
				break;
			default:
				break;
			}
		}
	}

	public void validateCommand() throws DBException {
		String passValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD));
		String confirmValue = CommonUtils.toString(getProperty(UserPropertyHandler.PASSWORD_CONFIRM));
		if (!CommonUtils.isEmpty(passValue) && !CommonUtils.equalObjects(passValue, confirmValue)) {
			throw new DBException("Password confirmation value is invalid");
		}
	}

	@Override
	public DBEPersistAction[] getPersistActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			Map<String, Object> options) {
		List<DBEPersistAction> actions = new ArrayList<>();
		boolean newUser = !getObject().isPersisted();
		// 创建新用户
		if (newUser) {
			actions.add(new SQLDatabasePersistAction(Messages.edit_command_change_user_action_create_new_user,
					"CREATE USER " + getObject().getName() + "\nIDENTIFIED BY "
							+ SQLUtils.quoteString(getObject(),
									CommonUtils.toString(getProperties().get(UserPropertyHandler.PASSWORD.name())))
							+ " ") { //$NON-NLS-1$
				@Override
				public void afterExecute(DBCSession session, Throwable error) {
					if (error == null) {
						getObject().setPersisted(true);
					}
				}
			});
		}
		// 修改旧用户
		else {
			StringBuilder script = new StringBuilder();
			boolean hasSet;
			hasSet = generateAlterScript(script);
			if (hasSet) {
				actions.add(new SQLDatabasePersistAction(Messages.edit_command_change_user_action_update_user_record,
						script.toString()));
			}
			// 对角色做额外处理
			String oldRole = getObject().getRoleList();
			String[] oldRoleList = oldRole == null ? null : oldRole.split(",");
			String[] newRoleList = (String[]) getProperties().get("ROLE_LIST");
			// 只有newRoleList不为空时才进行处理
			if (newRoleList != null) {
				// 对每一个新角色都去旧角色列表进行查找
				for (int i = 0, l = newRoleList.length; i < l; i++) {
					boolean inRoleListFlag = false;
					// 没有已有角色则直接添加
					if (oldRoleList == null || oldRoleList.length == 0) {
						actions.add(new SQLDatabasePersistAction("Grant new role to user",
								"GRANT ROLE " + newRoleList[i] + " TO " + getObject().getName()));
					}
					// 否则需要先检查新角色是否已有
					else { 
						for (int j = 0, l2 = oldRoleList.length; j < l2; j++) {
							if (newRoleList[i].equals(oldRoleList[j])) {
								inRoleListFlag = true;
								break;
							}
						}
						// 新角色不在列表中则为用户添加角色
						if (!inRoleListFlag) {
							actions.add(new SQLDatabasePersistAction("Grant new role to user",
									"GRANT ROLE " + newRoleList[i] + " TO " + getObject().getName()));
						}
					}
				}
				for (int i = 0, l = oldRoleList.length; i < l; i++) {
					boolean inRoleListFlag = false;
					// 没有新角色则直接删除
					if (newRoleList == null || newRoleList.length == 0) {
						if(!"".equals(oldRoleList[i])) {
							actions.add(new SQLDatabasePersistAction("Revoke old role from user",
									"REVOKE ROLE " + oldRoleList[i] + " FROM " + getObject().getName()));
						}
					}
					// 否则需要检查旧角色是否还保有
					else {
						for (int j = 0, l2 = newRoleList.length; j < l2; j++) {
							if (oldRoleList[i].equals(newRoleList[j])) {
								inRoleListFlag = true;
								break;
							}
						}
						// 旧角色不在列表中则为用户删除角色
						if (!inRoleListFlag) {
							if(!"".equals(oldRoleList[i])) {
								actions.add(new SQLDatabasePersistAction("Revoke old role from user",
										"REVOKE ROLE " + oldRoleList[i] + " FROM " + getObject().getName()));
							}
						}
					}
				}
			}

			// 对权限做额外处理
			// 库级权限
			Collection<UserAuthority> oldAuthorities = getObject().getUserDatabaseAuthorities();
			// 对象级权限
			Collection<UserAuthority> oldAuthorities2 = getObject().getUserObjectAuthorities();
			// 二级对象权限
			Collection<UserAuthority> oldAuthorities3 = getObject().getUserSubObjectAuthorities();
			Iterator<UserAuthority> it =null;
			if(oldAuthorities3!=null) {
				it = oldAuthorities3.iterator();
			}
			if(it!=null) {
				while (it.hasNext()) {
					UserAuthority authority = it.next();
					if (!(authority.getName().contains("列"))) {
						oldAuthorities3.remove(authority);
					}
				}
			}
			String schema = "";
			String object = "";
			String realTargetName = "";
			String[] newAuthorities = null;
			UserAuthority authority = null;
			for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
				switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
				case DATABASE_AUTHORITY:
					// 遍历新权限列表，若旧权限不存在于其中，则做revoke操作
					if(oldAuthorities==null) {
						break;
					}
					it = oldAuthorities.iterator();
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
							actions.add(new SQLDatabasePersistAction("Revoke user",
									"REVOKE " + Utils.transformAuthority(authority.getName(), true) + " FROM "
											+ getObject().getName()));
						}
					}
					// 遍历旧权限列表，若新权限不存在于其中，则做grant操作
					if(oldAuthorities==null) {
						break;
					}
					it = oldAuthorities.iterator();
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
					if(oldAuthorities2==null) {
						break;
					}
					it = oldAuthorities2.iterator();
					newAuthorities = (String[]) entry.getValue();
					schema = getProperties().get("TARGET_SCHEMA").toString();
					object = getProperties().get("TARGET_OBJECT").toString();
					realTargetName = "\"" + schema + "\".\"" + object + "\"";
					// 遍历新权限列表，若旧权限不存在于其中，则做revoke操作
					while (it.hasNext()) {
						authority = it.next();
						boolean inListFlag = false;
						//新权限在旧权限列表中
						for (int i = 0, l = newAuthorities.length; i < l; i++) {
							if(!newAuthorities[i].contains("\"")) {
								newAuthorities[i] = newAuthorities[i]+":"+realTargetName;
							}
							if (authority.getName().contains(newAuthorities[i])) {
//									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}
						// 旧权限不在新列表中则revoke
						if (!inListFlag && authority != null) {
							actions.add(new SQLDatabasePersistAction("Revoke user",
									"REVOKE " + Utils.transformAuthority(authority.getName(), false) + " " + "\""
											+ schema + "\".\"" + object + "\"" + " FROM " + getObject().getName()));
						}
					}
					// 遍历旧权限列表，若新权限不存在于其中，则做grant操作
					if(oldAuthorities2==null) {
						break;
					}
					it = oldAuthorities2.iterator();
					for (int i = 0, l = newAuthorities.length; i < l; i++) {
						boolean inListFlag = false;
						while (it.hasNext()) {
							authority = it.next();
							if (authority.getName().contains(newAuthorities[i])) {
//									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}
						// 新权限不在列表中则grant
						if (!inListFlag) {
							String authorityName = newAuthorities[i].substring(newAuthorities[i].indexOf(":")+1, newAuthorities[i].length());
							actions.add(new SQLDatabasePersistAction("Grant user",
									"GRANT " + Utils.transformAuthority(newAuthorities[i], false) + " " + authorityName
											+ " TO " + getObject().getName()));
						}
					}
					break;
				case SUB_OBJECT_AUTHORITY:
					if(oldAuthorities3==null) {
						break;
					}
					it = oldAuthorities3.iterator();
					newAuthorities = (String[]) entry.getValue();
					schema = getProperties().get("TARGET_SCHEMA").toString();
					object = getProperties().get("TARGET_OBJECT").toString();
					String subObject = getProperties().get("SUB_TARGET_OBJECT").toString();
					String subObjectType = getProperties().get("SUB_TARGET_TYPE").toString();
					realTargetName = "\"" + schema + "\".\"" + object + "\"" + ".\"" + subObject + "\"";

					// 遍历新权限列表，若旧权限不存在于其中，则做revoke操作
					while (it.hasNext()) {
						authority = it.next();
						boolean inListFlag = false;
						for (int i = 0, l = newAuthorities.length; i < l; i++) {
							if (authority.getName().contains(newAuthorities[i])
									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}
						if(inListFlag) {
							continue;
						}
						// 旧权限不在列表中则revoke
						if (!inListFlag && authority != null) {
							if (!"COLUMN".equals(subObjectType)) {
								actions.add(new SQLDatabasePersistAction("Revoke user",
										"REVOKE " + Utils.transformAuthority(authority.getName(), false) + " " + "\""
												+ schema + "\".\"" + object + "\"" + " FROM " + getObject().getName()));
							}
							// 对列对象做特殊处理
							else {
								actions.add(new SQLDatabasePersistAction("Revoke user",
										"REVOKE " + Utils.transformColumnAuthority(authority.getName()) + "("
												+ subObject + ") ON " + "\"" + schema + "\".\"" + object + "\""
												+ " FROM " + getObject().getName()));
							}
						}
					}
					// 遍历旧权限列表，若新权限不存在于其中，则做grant操作
					it = oldAuthorities3.iterator();
					for (int i = 0, l = newAuthorities.length; i < l; i++) {
						boolean inListFlag = false;
						while (it.hasNext()) {
							authority = it.next();
							if (authority.getName().contains(newAuthorities[i])
									&& authority.getTargetName().equals(realTargetName)) {
								inListFlag = true;
								break;
							}
						}
						if(inListFlag) {
							continue;
						}
						// 新权限不在列表中则grant
						if (!inListFlag) {
							if (!"COLUMN".equals(subObjectType)) {
								actions.add(new SQLDatabasePersistAction("Grant user",
										"GRANT " + Utils.transformAuthority(newAuthorities[i], false) + " "
												+ realTargetName + " TO " + getObject().getName()));
							}
							// 对列类型做特殊处理
							else {
								actions.add(new SQLDatabasePersistAction("Grant user",
										"GRANT " + Utils.transformColumnAuthority(newAuthorities[i]) + "(" + subObject
												+ ") ON " + "\"" + schema + "\".\"" + object + "\"" + " TO "
												+ getObject().getName()));
							}
						}
					}
					break;
				default:
					break;
				}
			}
		}
		return actions.toArray(new DBEPersistAction[actions.size()]);
	}

	private boolean generateAlterScript(StringBuilder script) {
		boolean hasSet = false;
		script.append("ALTER USER ").append(getObject().getName());
		String passwordString = "";
		for (Map.Entry<Object, Object> entry : getProperties().entrySet()) {
			switch (UserPropertyHandler.valueOf((String) entry.getKey())) {
			
			// 处理密码更改
			case PASSWORD:
				script.append("\nIDENTIFIED BY ")
						.append(SQLUtils.quoteString(getObject(), CommonUtils.toString(entry.getValue())));
				hasSet = true;
				passwordString = CommonUtils.toString(entry.getValue());
				break;
				
			case PASSWORD_CONFIRM:
				String confirmString = CommonUtils.toString(entry.getValue());
				if (!Objects.equals(passwordString, confirmString)) {
					 throw new RuntimeException("确认密码错误，请重新输入！");
				}
				break;
			// 处理用户名更改
			case NAME:
				script.append("\nRENAME TO ").append(CommonUtils.toString(entry.getValue()));
				hasSet = true;
				break;
			
			// 处理加锁
			case LOCKED:
				script.append("\nACCOUNT ");
				script.append(CommonUtils.toBoolean(entry.getValue()) ? "LOCK" : "UNLOCK");
				hasSet = true;
				break;
			
			// 处理密码失效
			case EXPIRED:
				script.append(CommonUtils.toBoolean(entry.getValue()) ? "\nPASSWORD EXPIRE" : "");
				hasSet = CommonUtils.toBoolean(entry.getValue()) ? true : hasSet;
				break;
			
			// 处理密码失效
			case UNTIL_TIME:
				script.append("\nVALID UNTIL '").append(CommonUtils.toString(entry.getValue())).append("'");
				hasSet = true;
				break;
			default:
				break;
			}
		}
		return hasSet;
	}

}