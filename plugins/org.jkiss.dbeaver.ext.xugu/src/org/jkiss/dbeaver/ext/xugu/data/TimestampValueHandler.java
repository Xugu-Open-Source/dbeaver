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
package org.jkiss.dbeaver.ext.xugu.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.time.ExtendedDateFormat;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间戳值处理器
 */
public class TimestampValueHandler extends JDBCDateTimeValueHandler {
	private static final SimpleDateFormat DEFAULT_DATETIME_FORMAT = new ExtendedDateFormat(
			"''yyyy-MM-dd HH:mm:ss''");
	private static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("''yyyy-MM-dd''");
	private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat("''HH:mm:ss.SSS''");

	// private static Method TIMESTAMP_READ_METHOD = null, TIMESTAMPTZ_READ_METHOD =
	// null, TIMESTAMPLTZ_READ_METHOD = null;

	public TimestampValueHandler(DBDFormatSettings formatSettings)
    {
        super(formatSettings);
    }

	@Override
	public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object,
			boolean copy, boolean validateValue) throws DBCException {
		return super.getValueFromObject(session, type, object, copy, validateValue);
	}

	@Nullable
	@Override
	public Format getNativeValueFormat(DBSTypedObject type) {
		switch (type.getTypeID()) {
		case Types.TIMESTAMP:
			return DEFAULT_DATETIME_FORMAT;
		case Types.TIMESTAMP_WITH_TIMEZONE:
		case Constants.DATA_TYPE_TIMESTAMP_WITH_TIMEZONE:
		case Constants.DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE:
			return DEFAULT_DATETIME_FORMAT;
		case Types.TIME:
			return DEFAULT_TIME_FORMAT;
		case Types.TIME_WITH_TIMEZONE:
			return DEFAULT_TIME_FORMAT;
		case Types.DATE:
			return DEFAULT_DATE_FORMAT;
		default:
			break;
		}
		// Have to revert DATE format. I can't realize what is difference between
		// TIMESTAMP and DATE without time part.
		// Column types and lengths are the same. Data type name is the same. Oh,
		// Oracle...
		/*
		 * if (type.getMaxLength() == OracleConstants.DATE_TYPE_LENGTH) { return
		 * DEFAULT_DATE_FORMAT; }
		 */
		return super.getNativeValueFormat(type);
	}

	@Override
	protected String getFormatterId(DBSTypedObject column) {
		/*
		 * if (column.getMaxLength() == OracleConstants.DATE_TYPE_LENGTH) { return
		 * DBDDataFormatter.TYPE_NAME_DATE; }
		 */
		return super.getFormatterId(column);
	}
	
    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        try {
            if (resultSet instanceof JDBCResultSet) {
                JDBCResultSet dbResults = (JDBCResultSet) resultSet;

                // check for native format
                if (formatSettings.isUseNativeDateTimeFormat()) {
                    try {
                        return dbResults.getString(index + 1);
                    } catch (SQLException e) {
                        log.debug("Can't read date/time value as string: " + e.getMessage());
                    }
                }

                // It seems that some drivers doesn't support reading date/time values with explicit calendar
                // So let's use simple version
                switch (type.getTypeID()) {
                    case Types.TIME:
                        return dbResults.getTime(index + 1);
                    case Types.DATE:
                        return dbResults.getDate(index + 1);
                    default:
                        Object value = dbResults.getObject(index + 1);
                        return getValueFromObject(session, type, value, false, false);
                }
            } else {
                return resultSet.getAttributeValue(index);
            }
        } catch (SQLException e) {
            try {
                if (e.getCause() instanceof ParseException ||
                    e.getCause() instanceof UnsupportedOperationException) {
                    // [SQLite] workaround.
                    Object objectValue = ((JDBCResultSet) resultSet).getObject(index + 1);
                    if (objectValue instanceof Date) {
                        return objectValue;
                    } else if (objectValue instanceof String) {
                        // Do not convert to Date object because table column has STRING type
                        // and it will be converted in string at late binding stage making incorrect string value: Date.toString()
                        return objectValue;
                    } else if (objectValue != null) {
                        // Perhaps some database-specific timestamp representation. E.lg. H2 TimestampWithTimezone
                        return objectValue.toString();
                    } else {
                        return null;
                    }
                } else if (
                    SQLState.SQL_42000.getCode().equals(e.getSQLState()) ||
                        SQLState.SQL_S1009.getCode().equals(e.getSQLState()) ||
                        SQLState.SQL_HY000.getCode().equals(e.getSQLState())) {
                    // [MySQL, Netezza] workaround. Time value may be interval (should be read as string)
                    return ((JDBCResultSet) resultSet).getString(index + 1);
                }
            } catch (SQLException e1) {
                // Ignore
                log.debug("Can't retrieve datetime object", e1);
                return null;
            }
            throw new DBCException(e, session.getExecutionContext());
        }
    }
}
