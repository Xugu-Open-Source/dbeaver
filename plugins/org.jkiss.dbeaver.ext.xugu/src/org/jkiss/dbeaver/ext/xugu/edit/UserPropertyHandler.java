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

import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyHandler;
import org.jkiss.dbeaver.model.edit.prop.DBEPropertyReflector;

/**
 * 用户属性处理器，将界面逻辑与处理逻辑进行映射
 */
public enum UserPropertyHandler implements DBEPropertyHandler<User>, DBEPropertyReflector<User> {
	/**
	 * 用户属性枚举
	 */
	NAME, HOST, PASSWORD, PASSWORD_CONFIRM, LOCKED, EXPIRED, UNTIL_TIME, ROLE_LIST, DATABASE_AUTHORITY,
	OBJECT_AUTHORITY, SUB_OBJECT_AUTHORITY, TARGET_SCHEMA, TARGET_TYPE, TARGET_OBJECT, SUB_TARGET_TYPE,
	SUB_TARGET_OBJECT, MAX_QUERIES, MAX_UPDATES, MAX_CONNECTIONS, MAX_USER_CONNECTIONS;

	@Override
	public Object getId() {
		return name();
	}

	@Override
	public CommandChangeUser createCompositeCommand(User object) {
		return new CommandChangeUser(object);
	}

	/**
	 * 为了修改用户名而保留旧名称 不做即时反射更新
	 */
	@Override
	public void reflectValueChange(User object, Object oldValue, Object newValue) {
	}
}
