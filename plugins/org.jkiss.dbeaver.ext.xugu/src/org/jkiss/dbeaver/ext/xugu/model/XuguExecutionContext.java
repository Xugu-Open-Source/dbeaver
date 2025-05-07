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
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * 执行上下文
 */
public class XuguExecutionContext extends JDBCExecutionContext implements DBCExecutionContextDefaults<DBSCatalog, XuguSchema> {
	private static final Log log = Log.getLog(XuguDataSource.class);

	private String activeSchemaName;

	public XuguExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
		super(instance, purpose);
	}

	@Override
	public DBSCatalog getDefaultCatalog() {
		// TODO 获取默认目录
		return null;
	}

	@Override
	public XuguSchema getDefaultSchema() {
		try {
			return activeSchemaName == null?null : (XuguSchema)getDataSource().getSchemaCache().getCachedObject(activeSchemaName);
//			return activeSchemaName == null?null : new XuguSchema(getDataSource(), activeSchemaName,  true);
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
	public void setDefaultCatalog(DBRProgressMonitor monitor, DBSCatalog catalog, XuguSchema schema) throws DBCException {
		throw new DBCFeatureNotSupportedException();
	}

	@Override
	public void setDefaultSchema(DBRProgressMonitor monitor, XuguSchema schema) throws DBCException {
		final XuguSchema oldSelectedEntity = getDefaultSchema();
		if (schema == null || oldSelectedEntity == schema) {
			return;
		}
		setCurrentSchema(monitor, schema.getName());
		activeSchemaName = schema.getName();
		// 发送通知
//		DBUtils.fireObjectSelectionChange(oldSelectedEntity, schema,this);
	}

	@Override
	public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
		// 检查默认激活模式
		try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active schema")) {
			if (useBootstrapSettings) {
				DBPConnectionBootstrap bootstrap = getBootstrapSettings();
				String bootstrapSchemaName = bootstrap.getDefaultSchemaName();
				if (!CommonUtils.isEmpty(bootstrapSchemaName) && !bootstrapSchemaName.equals(activeSchemaName)) {
					setCurrentSchema(monitor, bootstrap.getDefaultSchemaName());
				}
			}
			// 获取激活模式
			this.activeSchemaName =getCurrentSchema(session, null);
		} catch (Exception e) {
			throw new DBCException(e, this);
		}

		return true;
	}

	@NotNull
	@Override
	public XuguDataSource getDataSource() {
		return (XuguDataSource) super.getDataSource();
	}

	@NotNull
	@Override
	public XuguExecutionContext getContextDefaults() {
		return this;
	}

	public String getActiveSchemaName() {
		return activeSchemaName;
	}





	//fixme 获取当前激活模式
	public static String getCurrentSchema(JDBCSession session, String role) throws SQLException {
		String sql = "SHOW CURRENT_SCHEMA";
		JDBCStatement s = session.createStatement();
		JDBCResultSet rs = s.executeQuery(sql);
		if (rs.next()) {
			String res = rs.getString(1);
			rs.close();
			s.close();
			return res;
		} else {
			rs.close();
			s.close();
			return null;
		}
	}


	private void setCurrentSchema(DBRProgressMonitor monitor, String activeSchemaName) throws DBCException {
		DBSObject oldDefaultSchema = getDefaultSchema();
		try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active schema")) {
			JDBCUtils.executeStatement(session, "SET CURRENT_SCHEMA TO " +
					DBUtils.getQuotedIdentifier(session.getDataSource(), activeSchemaName));
			this.activeSchemaName = activeSchemaName;
			DBSObject newDefaultSchema = getDefaultSchema();
			DBUtils.fireObjectSelectionChange(oldDefaultSchema, newDefaultSchema, this);
		} catch (SQLException e) {
			throw new DBCException(e, this);
		}
	}

	@NotNull
	@Override
	public DBCCachedContextDefaults getCachedDefault() {
		return new DBCCachedContextDefaults(null, activeSchemaName);
	}
}
