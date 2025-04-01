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
package org.jkiss.dbeaver.ext.cae.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;

/**
 * 对象值处理器
 */
public class ObjectValueHandler extends JDBCAbstractValueHandler {
	public static final ObjectValueHandler INSTANCE = new ObjectValueHandler();

	@NotNull
	@Override
	public String getValueDisplayString(@NotNull DBSTypedObject column, Object value,
			@NotNull DBDDisplayFormat format) {
		if (value != null) {
			return "[OBJECT]";
		} else {
			return super.getValueDisplayString(column, value, format);
		}
	}

	@Override
	protected ObjectValue fetchColumnValue(DBCSession session, JDBCResultSet resultSet, DBSTypedObject type, int index)
			throws DBCException, SQLException {
		Object object = resultSet.getObject(index);
		return (ObjectValue) getValueFromObject(session, type, object, false, false);
	}

	@Override
	protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
			int paramIndex, Object value) throws DBCException, SQLException {
		throw new DBCException("Parameter bind is not implemented");
	}

	@NotNull
	@Override
	public Class<Object> getValueObjectType(@NotNull DBSTypedObject attribute) {
		return java.lang.Object.class;
	}

	@Override
	public Object getValueFromObject(DBCSession session, DBSTypedObject type, Object object, boolean copy,
			boolean validateValue) throws DBCException {
		if (object == null) {
			return new ObjectValue(null);
		} else if (object instanceof ObjectValue) {
			return copy ? new ObjectValue(((ObjectValue) object).getValue()) : (ObjectValue) object;
		} else {
			return new ObjectValue(object);
		}
	}
}
