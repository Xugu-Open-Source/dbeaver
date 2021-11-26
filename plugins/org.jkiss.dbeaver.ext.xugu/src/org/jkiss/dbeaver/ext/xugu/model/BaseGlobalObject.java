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

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * 基本全局对象
 */
public abstract class BaseGlobalObject implements DBSObject, DBPSaveableObject {
	private static final Log log = Log.getLog(BaseGlobalObject.class);

	private final DataSource dataSource;
	private boolean persisted;

	protected BaseGlobalObject(DataSource dataSource, boolean persisted) {
		this.dataSource = dataSource;
		this.persisted = persisted;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	/**
	 * 返回父对象
	 */
	@Override
	public DBSObject getParentObject() {
		return dataSource.getContainer();
	}

	/**
	 * 返回数据源
	 */
	@NotNull
	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * 是否存在
	 */
	@Override
	public boolean isPersisted() {
		return persisted;
	}

	/**
	 * 设置是否存在
	 */
	@Override
	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}
}
