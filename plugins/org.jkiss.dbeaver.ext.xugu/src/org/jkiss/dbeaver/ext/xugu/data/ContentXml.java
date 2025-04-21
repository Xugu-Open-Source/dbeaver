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
package org.jkiss.dbeaver.ext.xugu.data;

import org.jkiss.dbeaver.ext.xugu.internal.Constants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentXML;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.BeanUtils;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * XML 内容
 */
public class ContentXml extends JDBCContentXML {
	public ContentXml(DBCExecutionContext executionContext, SQLXML xml)
    {
        super(executionContext, xml);
    }

	@Override
	protected ContentXml createNewContent() {
		return new ContentXml(executionContext, null);
	}

	@Override
	public void bindParameter(JDBCSession session, JDBCPreparedStatement preparedStatement, DBSTypedObject columnType,
			int paramIndex) throws DBCException {
		try {
			if (storage != null) {
				try (InputStream streamReader = storage.getContentStream()) {
					final Object xmlObject = createXmlObject(session, streamReader);

					preparedStatement.setObject(paramIndex, xmlObject);
				}
			} else {
				preparedStatement.setNull(paramIndex, java.sql.Types.SQLXML);
			}
		}
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
        catch (IOException e) {
            throw new DBCException("IO error while reading XML", e);
        }
	}

	private Object createXmlObject(JDBCSession session, InputStream stream) throws DBCException {
		try {
			return BeanUtils.invokeStaticMethod(DBUtils.getDriverClass(session.getExecutionContext().getDataSource(), Constants.XMLTYPE_CLASS_NAME),
					"createXML", new Class[] { java.sql.Connection.class, java.io.InputStream.class },
					new Object[] { session.getOriginal(), stream });
		} catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        } catch (Throwable e) {
            throw new DBCException("Internal error when creating XMLType", e, session.getExecutionContext());
        }
	}
}
