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
package org.jkiss.dbeaver.ext.xugu.data.content;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.edit.AbstractObjectManager;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentLOB;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基本内容封装
 */
public abstract class BaseContentOpaque<OPAQUE_TYPE extends Object> extends JDBCContentLOB {
	protected static final Log LOG = Log.getLog(AbstractObjectManager.class);

	private OPAQUE_TYPE opaque;
	private InputStream tmpStream;

	public BaseContentOpaque(DBCExecutionContext executionContext, OPAQUE_TYPE opaque) {
        super(executionContext);
        this.opaque = opaque;
    }

	@Override
	public long getLOBLength() throws DBCException {
		// TODO 获取 LOB 长度
		return 0;
	}

	@NotNull
	@Override
	public String getContentType() {
		return MimeTypes.TEXT_XML;
	}

	@Override
	public DBDContentStorage getContents(DBRProgressMonitor monitor) throws DBCException {
		if (storage == null && opaque != null) {
			storage = makeStorageFromOpaque(monitor, opaque);
			opaque = null;
		}
		return storage;
	}

	@Override
	public void release() {
		if (tmpStream != null) {
			ContentUtils.close(tmpStream);
			tmpStream = null;
		}
		super.release();
	}

	@Override
	public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType,
			int paramIndex) throws DBCException {
		try {
			if (storage != null) {
				preparedStatement.setObject(paramIndex, createNewObject(session.getOriginal()));
			} else if (opaque != null) {
				preparedStatement.setObject(paramIndex, opaque);
			} else {
				preparedStatement.setNull(paramIndex, java.sql.Types.SQLXML);
			}
		} catch (IOException e) {
			throw new DBCException("IO error while reading content", e);
		} catch (SQLException e) {
			throw new DBCException(e, session.getExecutionContext());
		}
	}

	@Override
	public boolean isNull() {
		return opaque == null && storage == null;
	}

	@Override
	public String getDisplayString(DBDDisplayFormat format) {
		return opaque == null && storage == null ? null : "[" + getOpaqueType() + "]";
	}

	/**
	 * 获取封装类型
	 * 
	 * @return 封装类型名称
	 */
	protected abstract String getOpaqueType();

	/**
	 * 创建新对象
	 * 
	 * @param connection 数据库连接
	 * @return 封装类型
	 * @throws DBCException 数据库客户端异常
	 * @throws IOException  文件读写异常
	 * @throws SQLException 数据库异常
	 */
	protected abstract OPAQUE_TYPE createNewObject(Connection connection)
			throws DBCException, IOException, SQLException;

	/**
	 * 根据封装类型创建存储
	 * 
	 * @param monitor 进程监视器
	 * @param opaque  封装类型
	 * @return 内容存储对象
	 * @throws DBCException 数据库客户端异常
	 */
	protected abstract DBDContentStorage makeStorageFromOpaque(DBRProgressMonitor monitor, OPAQUE_TYPE opaque)
			throws DBCException;
}
