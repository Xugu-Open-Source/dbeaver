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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * 用户权限和角色权限的基类
 * 
 * @author Xugu
 */
public abstract class BaseAuthority<PARENT extends DBSObject> extends BaseObject<PARENT> {
	private String parentName;
	protected String targetName;
	/**
	 * 是否为库级权限
	 */
	protected boolean isDatabase;

	protected BaseAuthority(PARENT parent, String name, String targetName, boolean isDatabase, boolean persisted) {
		super(parent, name, persisted);
		this.parentName = parent.getName();
		this.targetName = targetName;
		this.isDatabase = isDatabase;
	}

	public boolean isDatabase() {
		return this.isDatabase;
	}

	public String getParentName() {
		return this.parentName;
	}
}
