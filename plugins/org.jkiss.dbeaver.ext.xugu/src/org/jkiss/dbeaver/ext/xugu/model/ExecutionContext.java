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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * 执行上下文
 */
public class ExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<DBSCatalog, Schema> {
	private static final Log log = Log.getLog(ExecutionContext.class);

	private String activeSchemaName;

	ExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
		super(instance, purpose);
	}

	@NotNull
	@Override
	public DataSource getDataSource() {
		return (DataSource) super.getDataSource();
	}

	@NotNull
	@Override
	public ExecutionContext getContextDefaults() {
		return this;
	}

	public String getActiveSchemaName() {
		return activeSchemaName;
	}

	@Override
	public DBSCatalog getDefaultCatalog() {
		// TODO 获取默认目录
		return null;
	}

	@Override
	public Schema getDefaultSchema() {
		try {
			return activeSchemaName == null ? null
					: getDataSource().getSchema(new VoidProgressMonitor(), activeSchemaName);
		} catch (Exception e) {
			log.error(e);
			return null;
		}
	}

	@Override
	public boolean supportsCatalogChange() {
		return false;
	}

	@Override
	public boolean supportsSchemaChange() {
		return true;
	}

	@Override
	public void setDefaultCatalog(DBRProgressMonitor monitor, DBSCatalog catalog, Schema schema) throws DBCException {
		throw new DBCFeatureNotSupportedException();
	}

	@Override
	public void setDefaultSchema(DBRProgressMonitor monitor, Schema schema) throws DBCException {
		final Schema oldSelectedEntity = getDefaultSchema();
		if (schema == null || oldSelectedEntity == schema) {
			return;
		}
		setCurrentSchema(monitor, schema);
		activeSchemaName = schema.getName();

		// 发送通知
		DBUtils.fireObjectSelectionChange(oldSelectedEntity, schema,this);
	}

	@Override
	public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
		// 检查默认激活模式
		try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active schema")) {
			if (useBootstrapSettings) {
				DBPConnectionBootstrap bootstrap = getBootstrapSettings();
				if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName())) {
					setCurrentSchema(monitor, bootstrap.getDefaultSchemaName());
				}
			}
			// 获取激活模式
			this.activeSchemaName = Utils.getCurrentSchema(session, null);
			if (this.activeSchemaName != null) {
				if (this.activeSchemaName.isEmpty()) {
					this.activeSchemaName = null;
				}
			}
		} catch (Exception e) {
			throw new DBCException(e, this);
		}

		return true;
	}

	void setCurrentSchema(DBRProgressMonitor monitor, Schema object) throws DBCException {
		if (object == null) {
			log.debug("Null current schema");
			return;
		}
		setCurrentSchema(monitor, object.getName());
	}

	private void setCurrentSchema(DBRProgressMonitor monitor, String activeSchemaName) throws DBCException {
		try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
			Utils.setCurrentSchema(session, activeSchemaName);
			this.activeSchemaName = activeSchemaName;
		} catch (SQLException e) {
			throw new DBCException(e, this);
		}
	}
}
