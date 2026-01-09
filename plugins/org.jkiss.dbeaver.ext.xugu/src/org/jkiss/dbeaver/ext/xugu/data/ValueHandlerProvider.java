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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueHandlerProvider;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCContentValueHandler;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStandardValueHandlerProvider;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Types;

/**
 * 值处理器提供者
 */
public class ValueHandlerProvider implements DBDValueHandlerProvider {
	@Override
	public DBDValueHandler getValueHandler(DBPDataSource dataSource, DBDFormatSettings preferences, DBSTypedObject typedObject)
    {
		switch (typedObject.getTypeID()) {
		case Types.BLOB:
			return BlobValueHandler.INSTANCE;
		case Types.CLOB:
		case Types.NCLOB:
			return ClobValueHandler.INSTANCE;
		case Types.STRUCT:
			return ObjectValueHandler.INSTANCE;
		case Types.ARRAY:
			return ArrayValueHandler.INSTANCE;
		default:
			break;
		}

		final String typeName = typedObject.getTypeName().toUpperCase();
		switch (typeName) {
		case Constants.TYPE_NAME_JSON:
            return JDBCContentValueHandler.INSTANCE;
		case Constants.TYPE_NAME_XML:
		case Constants.TYPE_NAME_XMLTYPE:
			return XmlValueHandler.INSTANCE;
		case Constants.TYPE_NAME_BFILE:
			return BfileValueHandler.INSTANCE;
		case Constants.TYPE_NAME_GEOMETRY:
			return GeometryValueHandler.INSTANCE;
		case Constants.TYPE_NAME_BIT:
		case Constants.TYPE_NAME_VARBIT:
			return BitValueHandler.INSTANCE;
		default:
			break;
		}

		if (typeName.contains(Constants.TYPE_NAME_TIMESTAMP) || typedObject.getDataKind() == DBPDataKind.DATETIME) {
			return new TimestampValueHandler(preferences,dataSource);
		}

		return new JDBCStandardValueHandlerProvider().getValueHandler(dataSource, preferences, typedObject);
	}
}