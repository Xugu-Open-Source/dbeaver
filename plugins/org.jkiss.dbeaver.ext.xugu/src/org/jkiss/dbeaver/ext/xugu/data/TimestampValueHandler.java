/*
. * DBeaver - Universal Database Manager
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
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDFormatSettings;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCDateTimeValueHandler;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

/**
 * 时间戳值处理器
 */
public class TimestampValueHandler extends JDBCDateTimeValueHandler {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd G", Locale.ENGLISH);
	private static final DateTimeFormatter DATETIME_FORMATTER_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S G", Locale.ENGLISH);
	private static final DateTimeFormatter DATETIME_FORMATTER_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS G", Locale.ENGLISH);
	private static final DateTimeFormatter DATETIME_FORMATTER_3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS G", Locale.ENGLISH);
	private static final DateTimeFormatter TIME_WITH_TIMEZONE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss xxx", Locale.ENGLISH);
	private static final DateTimeFormatter DATETIME_WITH_TIMEZONE_FORMATTER_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S xxx G", Locale.ENGLISH);
	private static final DateTimeFormatter DATETIME_WITH_TIMEZONE_FORMATTER_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SS xxx G", Locale.ENGLISH);
	private static final DateTimeFormatter DATETIME_WITH_TIMEZONE_FORMATTER_3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS xxx G", Locale.ENGLISH);
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd G", Locale.ENGLISH);
	private static final SimpleDateFormat DATETIME_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S G", Locale.ENGLISH);
	private static final SimpleDateFormat DATETIME_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS G", Locale.ENGLISH);
	private static final SimpleDateFormat DATETIME_FORMAT_3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS G", Locale.ENGLISH);
	private static final SimpleDateFormat TIME_WITH_TIMEZONE_FORMAT = new SimpleDateFormat("HH:mm:ss XXX", Locale.ENGLISH);
	private static final SimpleDateFormat DATETIME_WITH_TIMEZONE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S XXX G", Locale.ENGLISH);
	private static final SimpleDateFormat DATETIME_WITH_TIMEZONE_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS XXX G", Locale.ENGLISH);
	private static final SimpleDateFormat DATETIME_WITH_TIMEZONE_FORMAT_3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS XXX G", Locale.ENGLISH);

	// private static Method TIMESTAMP_READ_METHOD = null, TIMESTAMPTZ_READ_METHOD =
	// null, TIMESTAMPLTZ_READ_METHOD = null;

	public TimestampValueHandler(DBDFormatSettings formatSettings)
    {
        super(formatSettings);
    }

	@Override
	public String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format) {
		if (value instanceof TemporalAccessor) {
			String result;
			TemporalAccessor realValue = (TemporalAccessor) value;
			switch(column.getTypeID()) {
		        case Types.TIME:
		        	result = TIME_FORMATTER.format(realValue);
		            break;
		        case Types.DATE:
		        	result = DATE_FORMATTER.format(realValue);
		            break;
		        case Types.TIMESTAMP:
		        	try { result = DATETIME_FORMATTER_3.format(realValue); }
		        	catch (DateTimeException e1) {
		        		try { result = DATETIME_FORMATTER_2.format(realValue); }
			        	catch (DateTimeException e2) {
			        		try { result = DATETIME_FORMATTER_1.format(realValue); }
				        	catch (DateTimeException e3) {
				        		throw new IllegalStateException(e3);
				        	}
			        	}
		        	}
		            break;
		        case Types.TIME_WITH_TIMEZONE:
		        	result = TIME_WITH_TIMEZONE_FORMATTER.format(realValue);
		        	break;
		        case Types.TIMESTAMP_WITH_TIMEZONE:
		        	try { result = DATETIME_WITH_TIMEZONE_FORMATTER_3.format(realValue); }
		        	catch (DateTimeException e1) {
		        		try { result = DATETIME_WITH_TIMEZONE_FORMATTER_2.format(realValue); }
			        	catch (DateTimeException e2) {
			        		try { result = DATETIME_WITH_TIMEZONE_FORMATTER_1.format(realValue); }
				        	catch (DateTimeException e3) {
				        		throw new IllegalStateException(e3);
				        	}
			        	}
		        	}
		        	break;
		        default:
		        	result = super.getValueDisplayString(column, value, format);
			}
			if (format == DBDDisplayFormat.NATIVE && !result.startsWith("'") && !result.endsWith("'")) {
	            return "'" + result + "'";
			} else {
				return result;
			}
		} else if (value instanceof Date) {
			String result;
			Date realValue = (Date) value;
			switch(column.getTypeID()) {
		        case Types.TIME:
		        	result = TIME_FORMAT.format(realValue);
		            break;
		        case Types.DATE:
		        	result = DATE_FORMAT.format(realValue);
		            break;
		        case Types.TIMESTAMP:
		        	try { result = DATETIME_FORMAT_3.format(realValue); }
		        	catch (DateTimeException e1) {
		        		try { result = DATETIME_FORMAT_2.format(realValue); }
			        	catch (DateTimeException e2) {
			        		try { result = DATETIME_FORMAT_1.format(realValue); }
				        	catch (DateTimeException e3) {
				        		throw new IllegalStateException(e3);
				        	}
			        	}
		        	}
		            break;
		        case Types.TIME_WITH_TIMEZONE:
		        	result = TIME_WITH_TIMEZONE_FORMAT.format(realValue);
		        	break;
		        case Types.TIMESTAMP_WITH_TIMEZONE:
		        	try { result = DATETIME_WITH_TIMEZONE_FORMAT_3.format(realValue); }
		        	catch (DateTimeException e1) {
		        		try { result = DATETIME_WITH_TIMEZONE_FORMAT_2.format(realValue); }
			        	catch (DateTimeException e2) {
			        		try { result = DATETIME_WITH_TIMEZONE_FORMAT_1.format(realValue); }
				        	catch (DateTimeException e3) {
				        		throw new IllegalStateException(e3);
				        	}
			        	}
		        	}
		        	break;
		        default:
		        	result = super.getValueDisplayString(column, value, format);
			}
			if (format == DBDDisplayFormat.NATIVE && !result.startsWith("'") && !result.endsWith("'")) {
	            return "'" + result + "'";
			} else {
				return result;
			}
		} else {
			return super.getValueDisplayString(column, value, format);
		}
	}

	@Override
	public void bindValueObject(DBCSession session, DBCStatement statement, DBSTypedObject type, int index,
			Object value) throws DBCException {
        try {
            JDBCPreparedStatement dbStat = (JDBCPreparedStatement) statement;
            // JDBC uses 1-based indexes
            if (value == null) {
                dbStat.setNull(index + 1, type.getTypeID());
            } else if (value instanceof String) {
                // Some custom value format.
                dbStat.setString(index + 1, (String) value);
            } else if (value instanceof TemporalAccessor) {
    			TemporalAccessor realValue = (TemporalAccessor) value;
                switch (type.getTypeID()) {
                    case Types.TIME:
                        dbStat.setString(index + 1, TIME_FORMATTER.format(realValue));
                        break;
                    case Types.DATE:
                        dbStat.setString(index + 1, DATE_FORMATTER.format(realValue));
                        break;
        	        case Types.TIMESTAMP:
        	        	try { dbStat.setString(index + 1, DATETIME_FORMATTER_3.format(realValue)); }
    		        	catch (DateTimeException e1) {
    		        		try { dbStat.setString(index + 1, DATETIME_FORMATTER_2.format(realValue)); }
    			        	catch (DateTimeException e2) {
    			        		try { dbStat.setString(index + 1, DATETIME_FORMATTER_1.format(realValue)); }
    				        	catch (DateTimeException e3) {
    				        		throw new IllegalStateException(e3);
    				        	}
    			        	}
    		        	}
                        break;
                    case Types.TIME_WITH_TIMEZONE:
                        dbStat.setString(index + 1, TIME_WITH_TIMEZONE_FORMATTER.format(realValue));
                        break;
        	        case Types.TIMESTAMP_WITH_TIMEZONE:
        	        	try { dbStat.setString(index + 1, DATETIME_WITH_TIMEZONE_FORMATTER_3.format(realValue)); }
    		        	catch (DateTimeException e1) {
    		        		try { dbStat.setString(index + 1, DATETIME_WITH_TIMEZONE_FORMATTER_2.format(realValue)); }
    			        	catch (DateTimeException e2) {
    			        		try { dbStat.setString(index + 1, DATETIME_WITH_TIMEZONE_FORMATTER_1.format(realValue)); }
    				        	catch (DateTimeException e3) {
    				        		throw new IllegalStateException(e3);
    				        	}
    			        	}
    		        	}
                        break;
                    default:
                        dbStat.setTimestamp(index + 1, getTimestampValue(value));
                }
    		} else if (value instanceof Date) {
                switch (type.getTypeID()) {
	                case Types.TIME:
	                case Types.TIME_WITH_TIMEZONE:
	                    dbStat.setTime(index + 1, getTimeValue(value));
	                    break;
	                case Types.DATE:
	                    dbStat.setDate(index + 1, getDateValue(value));
	                    break;
	                default:
	                    dbStat.setTimestamp(index + 1, getTimestampValue(value));
	                    break;
                }
    		}
        } catch (SQLException e) {
            throw new DBCException(ModelMessages.model_jdbc_exception_could_not_bind_statement_parameter, e);
        }
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
                String stringValue = dbResults.getString(index + 1);
                if (stringValue == null) return null;
                Object objectValue = dbResults.getObject(index + 1);
                switch (type.getTypeID()) {
                    case Types.TIME:
                        return TIME_FORMATTER.parse(stringValue);
                    case Types.DATE:
                        return DATE_FORMATTER.parse(stringValue);
                    case Types.TIMESTAMP:
        	        	try { return DATETIME_FORMATTER_3.parse(stringValue); }
    		        	catch (DateTimeException e1) {
    		        		try { return DATETIME_FORMATTER_2.parse(stringValue); }
    			        	catch (DateTimeException e2) {
    			        		try { return DATETIME_FORMATTER_1.parse(stringValue); }
    				        	catch (DateTimeException e3) {
    				        		throw new IllegalStateException(e3);
    				        	}
    			        	}
    		        	}
                    case Types.TIME_WITH_TIMEZONE:
						String[] string = stringValue.split(" ");
						String time = string[0];
						char timezoneMark = string[1].charAt(0);
						String[] timezone = string[1].substring(1).split(":");
						Integer timezoneHour = Integer.parseInt(timezone[0]);
						Integer timezoneMinute = Integer.parseInt(timezone[1]);
						return TIME_WITH_TIMEZONE_FORMATTER.parse(
								String.format("%s %s%02d:%02d", time, timezoneMark, timezoneHour, timezoneMinute));
                    case Types.TIMESTAMP_WITH_TIMEZONE:
        	        	try { return DATETIME_WITH_TIMEZONE_FORMATTER_3.parse(stringValue); }
    		        	catch (DateTimeException e1) {
    		        		try { return DATETIME_WITH_TIMEZONE_FORMATTER_2.parse(stringValue); }
    			        	catch (DateTimeException e2) {
    			        		try { return DATETIME_WITH_TIMEZONE_FORMATTER_1.parse(stringValue); }
    				        	catch (DateTimeException e3) {
    				        		throw new IllegalStateException(e3);
    				        	}
    			        	}
    		        	}
                    default:
                        return objectValue;
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
