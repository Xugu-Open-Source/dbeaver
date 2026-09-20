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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.util.Collection;

/**
 * 包含数据库基本信息，其中模式信息由 dataSource 获取
 */
public class Database extends BaseGlobalObject {
	private DataSource dataSource;
	private int id;
	private String name;
	private String charset;
	private String timeZone;
	private boolean persisted;

	public Database(DataSource dataSource, String name) {
		super(dataSource, true);
		this.dataSource = dataSource;
		this.name = name;
	}

	public Database(DataSource dataSource, ResultSet dbResult) {
		super(dataSource, true);
		this.dataSource = dataSource;
		if (dbResult != null) {
			this.id = JDBCUtils.safeGetInt(dbResult, "DB_ID");
			this.name = JDBCUtils.safeGetString(dbResult, "DB_NAME");
			this.charset = JDBCUtils.safeGetString(dbResult, "DB_CHARSET");
			this.timeZone = JDBCUtils.safeGetString(dbResult, "DB_TIMEZ");
			persisted = true;
		} else {
			persisted = false;
		}
	}

	@Association
	public Collection<Schema> getSchemas(DBRProgressMonitor monitor) throws DBException {
		return dataSource.getSchemas(monitor);
	}

	@Association
	public Schema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
		return dataSource.getSchema(monitor, name);
	}

	@Override
	public DBSObject getParentObject() {
		return dataSource.getContainer();
	}

	@NotNull
	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Property(viewable = true, editable = false, order = 1)
	public int getId() {
		return id;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = false, updatable = false, order = 2)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Property(viewable = true, editable = true, order = 3)
	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Property(viewable = true, editable = true, order = 4)
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@Override
	public boolean isPersisted() {
		return persisted;
	}

	@Override
	public void setPersisted(boolean persisted) {
		this.persisted = persisted;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String toString() {
		return name + " [" + dataSource.getContainer().getName() + "]";
	}
}
