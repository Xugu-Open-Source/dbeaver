/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.gis.DBGeometry;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentAbstract;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCNumberValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;

/**
 * JDBCContentValueHandler
 */
public class BitValueHandler extends JDBCAbstractValueHandler {

	public static final BitValueHandler INSTANCE = new BitValueHandler();

	@Override
	public Class<?> getValueObjectType(DBSTypedObject attribute) {
		return String.class;
	}

	@Override
	public Object getValueFromObject(DBCSession session, DBSTypedObject type, Object object, boolean copy,
			boolean validateValue) throws DBCException {
		return object;
	}

	@Override
	protected Object fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
			throws DBCException, SQLException {
		return resultSet.getObject(index);
	}

	@Override
	protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
			int paramIndex, Object value) throws DBCException, SQLException {
		if (value instanceof JDBCContentAbstract) {
			((JDBCContentAbstract) value).bindParameter(session, statement, paramType, paramIndex);
		} else {
			throw new DBCException(ModelMessages.model_jdbc_unsupported_value_type_ + value);
		}

	}

}
