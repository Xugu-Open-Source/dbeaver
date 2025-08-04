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
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPIdentifierCase;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCSQLDialect;
import org.jkiss.dbeaver.model.impl.sql.BasicSQLDialect;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.data.content.BinaryFormatter;

import java.util.Arrays;
import java.util.Locale;

/**
 * SQL 方言
 */
class SqlDialect extends JDBCSQLDialect {
	public static final String[] EXEC_KEYWORDS = new String[] { "EXEC" };

	public static final String[] NON_TRANSACTIONAL_KEYWORDS = ArrayUtils.concatArrays(
			BasicSQLDialect.NON_TRANSACTIONAL_KEYWORDS,
			new String[] { "CREATE", "ALTER", "DROP", "ANALYZE", "VALIDATE", });

	public static final String[][] BEGIN_END_BLOCK = new String[][] {
			{ SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END }, { "IF", SQLConstants.BLOCK_END },
			{ "LOOP", SQLConstants.BLOCK_END + " LOOP" }, { "CASE", SQLConstants.BLOCK_END + " CASE" }, };

	public static final String[] BLOCK_HEADERS = new String[] { "DECLARE" };

	public static final String[] ADVANCED_KEYWORDS = { "PACKAGE", "FUNCTION", "TYPE", "TRIGGER", "IF", "EACH", "RETURN",
			"WRAPPED", "AFTER", "BEFORE", "DATABASE", "ANALYZE", "VALIDATE", "STRUCTURE", "COMPUTE", "STATISTICS",
			"LOOP", "WHILE", "BULK", "ELSIF", "EXIT", };

	private boolean crlfBroken;
	private DBPPreferenceStore preferenceStore;

	public SqlDialect() {
		super(OemConfig.OEM_NAME_EN, OemConfig.OEM_NAME_EN_LOWER);
	}

	@Override
    public void initDriverSettings(JDBCSession session, JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(session, dataSource, metaData);
		crlfBroken = !dataSource.isServerVersionAtLeast(11, 0);
		preferenceStore = dataSource.getContainer().getPreferenceStore();

		addFunctions(Arrays.asList("ABS", "ACOS", "ACOSD", "ACOSH", "ADDDATE", "ADDTIME", "ADD_MONTHS", "ASCII", "ASIN",
                "ASIND", "ASINH", "ATAN", "ATAN2", "ATAN2D", "ATAND", "ATANH", "ATOF", "ATOL", "BACKFILE_TYPE", "BASE64_DECODE",
                "BASE64_ENCODE", "BETWEEN00", "BETWEEN01", "BETWEEN10", "BETWEEN11", "BIN", "BITAND", "BITTOCHAR", "BIT_AND",
                "BIT_CLR", "BIT_COUNT", "BIT_LENGTH", "BIT_NOT", "BIT_OR", "BIT_SET", "BIT_TEST", "BIT_XOR", "BLEN", "BLENGTH",
                "BOX", "CBRT", "CEIL", "CEILING", "CHARACTER_LENGTH", "CHAR_LENGTH", "CHECK_AUTH", "CHECK_GROMETRY", "CHR",
                "CLR_VARS", "COMPARE", "CONCAT", "CONCAT_WS", "CONV", "CONVERT", "COS", "COSD", "COSH", "COT", "COTD", "CURDATE",
                "CURRENT_DATABASE", "CURRENT_DATE", "CURRENT_DATETIME", "CURRENT_DB", "CURRENT_DB_ID", "CURRENT_IP", "CURRENT_NODEID",
                "CURRENT_SCHEMA", "CURRENT_SCHEMAID", "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_USER", "CURRENT_USERID", "CURRVAL",
                "CURTIME", "DATABASE", "DATE", "DATEDIFF", "DATE_ADD", "DATE_FORMAT", "DATE_SUB", "DAY", "DAYNAME", "DAYOFMONTH",
                "DAYOFWEEK", "DAYOFYEAR", "DBTS", "DECODE_PG", "DEGREES", "DIR_EXISTS", "DIV", "DROP_FILE", "DROP_OS_FILE",
                "EMPTY_BLOB", "EMPTY_CLOB", "ENCODE_PG", "ERF", "ERFC", "ESCAPE_DECODE", "ESCAPE_ENCODE", "EXP", "EXTRACT",
                "EXTRACTVALUE", "EXTRACT_DAY", "EXTRACT_HOUR", "EXTRACT_MINUTE", "EXTRACT_MONTH", "EXTRACT_SECOND", "EXTRACT_YEAR",
                "FACTORIAL", "FILELEN", "FILE_EXISTS", "FIND_IN_SET", "FLOOR", "FORMAT_BINARY_TO_NUMBER", "FORMAT_GSTO_NOS",
                "FROM_BASE64", "FROM_DAYS", "FROM_UNIXTIME", "GCD", "GEN_RANDOM_UUID", "GETDATE", "GETDAY", "GETHOUR", "GETMINUTE",
                "GETMONTH", "GETSECOND", "GETTIME", "GETX", "GETY", "GETYEAR", "GET_BOOT_TIME", "GET_DVAR", "GET_FORMAT", "GET_FVAR",
                "GET_GATHER_NODE_NUM", "GET_HASH", "GET_INSTALL_PATH", "GET_IVAR", "GET_LICENSE_REMAIN_DAYS", "GET_STORE_NODE_NUM",
                "GET_SVAR", "GET_TYPE_SPACE", "GET_UPTIME", "GET_WORK_PATH", "HEADING", "HEX", "HEXTORAW", "HEX_DECODE", "HEX_ENCODE",
                "HOUR", "IMPORT_LICENSE", "INET_ATON", "INET_NTOA", "INITCAP", "INSERT", "INSTR", "INSTRB", "ISNULL", "IS_FALSE",
                "IS_ROLE_MEMBER", "IS_SYS_DBA", "IS_TRUE", "JSON_ARRAY", "JSON_ARRAY_APPEND", "JSON_ARRAY_INSERT", "JSON_CONTAINS",
                "JSON_CONTAINS_PATH", "JSON_DEPTH", "JSON_EXTRACT", "JSON_INSERT", "JSON_KEYS", "JSON_LENGTH", "JSON_MERGE",
                "JSON_MERGE_PATCH", "JSON_MERGE_PRESERVE", "JSON_OBJECT", "JSON_OVERLAPS", "JSON_PRETTY", "JSON_QUOTE",
                "JSON_REMOVE", "JSON_REPLACE", "JSON_SCHEMA_VALID", "JSON_SCHEMA_VALIDATION_REPORT", "JSON_SEARCH", "JSON_SET",
                "JSON_TYPE", "JSON_UNQUOTE", "JSON_VALID", "LABEL_CMP", "LABEL_FROM_CHAR", "LABEL_STR_CMP", "LABEL_TO_CHAR",
                "LAST_DAY", "LAST_INSERT_ID", "LCASE", "LCM", "LEFT", "LEFTB", "LEN", "LENGTH", "LENGTHB", "LN", "LOCALTIME",
                "LOCALTIMESTAMP", "LOCATE", "LOG", "LOG10", "LOG2", "LOWER", "LPAD", "LTRIM", "MAKEDATE", "MAKETIME",
                "MAKE_DATE", "MAKE_TIME", "MAKE_TIMESTAMP", "MAX_NODE_NUM", "MD5", "MD5_OLD", "MICROSECOND", "MID",
                "MINUTE", "MOD", "MONTH", "MONTHNAME", "MONTHS_BETWEEN", "MY_LABEL", "NANVL", "NEWID", "NEXTVAL",
                "NEXT_DAY", "NOW", "NUMTODSINTERVAL", "NUMTOYMINTERVAL", "NUM_NONNULLS", "NUM_NULLS", "OS_FILE_EXISTS",
                "OS_PATH", "OVERLAPS", "PERIOD_ADD", "PERIOD_DIFF", "PI", "PINYIN", "PINYIN1", "POINT", "POSITION", "POW",
                "POWER", "QUARTER", "RADIANS", "RAISE_APPLICATION_ERROR", "RAND", "RANDOM", "RANDOM_NORMAL", "RAWTOHEX",
                "REGEXP_COUNT", "REGEXP_INSTR", "REGEXP_LIKE", "REGEXP_REPLACE", "REGEXP_SUBSTR", "RELOAD_LICENSE", "REMAINDER",
                "RENAME_FILE", "RENAME_OS_FILE", "REPEAT", "REPLACE", "REPLICATE", "REVERSE", "REVERSE_STR", "RIGHT", "RIGHTB",
                "ROUND", "ROUND_TIES_TO_EVEN", "ROWIDTOCHAR", "RPAD", "RTRIM", "SECOND", "SEC_TO_TIME", "SEND_MSG", "SESSION_USER",
                "SETSEED", "SET_DVAR", "SET_FVAR", "SET_IVAR", "SET_SVAR", "SHL", "SHR", "SIGN", "SIN", "SIND", "SINH", "SLEEP",
                "SPACE", "SPLIT_PART", "SQLCODE", "SQLERRM", "SQRT", "SQUARE", "SRAND", "STRCMP", "STROF", "STUFF", "SUBDATE",
                "SUBSTR", "SUBSTRB", "SUBSTRING", "SUBSTRING_INDEX", "SUBTIME", "SYSDATE", "SYSDATETIME", "SYSTEM_USER", "SYSTIME",
                "SYSTIMESTAMP", "SYS_CONTEXT", "SYS_GUID", "SYS_USERID", "SYS_UUID", "TAILING", "TAN", "TAND", "TANH", "TIME",
                "TIMEDIFF", "TIMESTAMP", "TIMESTAMPADD", "TIMESTAMPDIFF", "TIME_FORMAT", "TIME_TO_SEC", "TO_BASE64", "TO_BLOB",
                "TO_CHAR", "TO_DATE", "TO_DAYS", "TO_HEX", "TO_NCHAR", "TO_NUMBER", "TO_SECONDS", "TO_TIMESTAMP", "TO_TIMESTAMPZ",
                "TO_XDATE", "TRANSACTION_TIMESTAMP", "TRANSLATE", "TRIM", "TRUNC", "TRUNCATE", "UCASE", "UID", "UNHEX",
                "UNIX_TIMESTAMP", "UPPER", "USER", "USERENV", "UTC_DATE", "UTC_TIME", "UTC_TIMESTAMP", "UUID", "VERSION",
                "WEEK", "WEEKDAY", "WEEKOFYEAR", "WRITE_MSG", "XMLATTRIBUTES", "XMLCAST", "XMLELEMENT", "XMLEXISTS",
                "XMLFOREST", "XMLQUERY", "XMLSEQUENCE", "XMLTABLE", "XMLTYPE", "YEAR", "YEARWEEK"));
		removeSQLKeyword("SYSTEM");

		for (String kw : ADVANCED_KEYWORDS) {
			addSQLKeyword(kw);
		}
	}

	@Override
	public String[][] getBlockBoundStrings() {
		return BEGIN_END_BLOCK;
	}


	// todo 大小写是否敏感问题
	@Override
	public boolean useCaseInsensitiveNameLookup() {
		return true;
	}

	@Override
	public String[] getBlockHeaderStrings() {
		return BLOCK_HEADERS;
	}

	@NotNull
	@Override
	public String[] getExecuteKeywords() {
		return EXEC_KEYWORDS;
	}

	@NotNull
	@Override
	public DBPIdentifierCase storesUnquotedCase() {
		return DBPIdentifierCase.MIXED;
	}

	/*@NotNull
	@Override
	public DBPIdentifierCase storesQuotedCase() {
		return DBPIdentifierCase.MIXED;
	}*/

	public static final String[][] XUGU_QUOTE_STRINGS = {
			{"`", "`"}
	};

	@Nullable
	@Override
	public String[][] getIdentifierQuoteStrings() {
		return XUGU_QUOTE_STRINGS;
	}

	@Override
	public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column,
			@NotNull String typeName, @NotNull DBPDataKind dataKind) {
		String ret = null;
		ret = super.getColumnTypeModifiers(dataSource, column, typeName, dataKind);
		typeName = CommonUtils.notEmpty(typeName).toUpperCase(Locale.ENGLISH);
		if (dataKind == DBPDataKind.STRING) {
			int precision = CommonUtils.toInt(column.getPrecision());
			if (precision > 1) {
				ret = "(" + precision + ')';
			}
		} else if (dataKind == DBPDataKind.DATETIME) {
			final String typeDayToSecond = "INTERVAL DAY TO SECOND";
			final String typeHourToSecond = "INTERVAL HOUR TO SECOND";
			final String typeMinuteToSecond = "INTERVAL MINUTE TO SECOND";
			final String typeYearToMonth = "INTERVAL YEAR TO MONTH";
			final String typeDayToHour = "INTERVAL DAY TO HOUR";
			final String typeDayToMinute = "INTERVAL DAY TO MINUTE";
			final String typeHourToMinute = "INTERVAL HOUR TO MINUTE";
			final String typeYear = "INTERVAL YEAR";
			final String typeMonth = "INTERVAL MONTH";
			final String typeDay = "INTERVAL DAY";
			final String typeHour = "INTERVAL HOUR";
			final String typeMinute = "INTERVAL MINUTE";
			final String typeSecond = "INTERVAL SECOND";
			final String typeTimestamp = "TIMESTAMP";

			if (typeSecond.equals(typeName) || typeDayToSecond.equals(typeName) || typeHourToSecond.equals(typeName)
					|| typeMinuteToSecond.equals(typeName)) {
				Integer scale = column.getScale();
				int precision = CommonUtils.toInt(column.getPrecision());
				if (precision == 0) {
					precision = (int) column.getMaxLength();
					if (precision > 0) {
						// FIXME: max length is actually length in character.
						// FIXME: On Oracle it returns bigger values than maximum (#1767)
						// FIXME: in other DBs it equals to precision in most cases
						// precision--; // One character for sign?
					}
				}
				final boolean isScaleExists = scale != null;
				final boolean isRightScale = isScaleExists && scale >= 0;
				final boolean isRightPrecision = precision >= 0;
				final boolean isBothZore = scale == 0 && precision == 0;
				if (isRightScale && isRightPrecision && !isBothZore) {
					ret = "(" + precision + ',' + scale + ')';
				}
			} else if (typeTimestamp.equals(typeName) || typeYear.equals(typeName) || typeMonth.equals(typeName)
					|| typeDay.equals(typeName) || typeHour.equals(typeName) || typeMinute.equals(typeName)
					|| typeYearToMonth.equals(typeName) || typeDayToHour.equals(typeName)
					|| typeDayToMinute.equals(typeName) || typeHourToMinute.equals(typeName)) {
				// Bit string?
				int precision = CommonUtils.toInt(column.getPrecision());
				if (precision > 1) {
					ret = "(" + precision + ')';
				}
			}
		}
		return ret;
	}

	@Override
	public boolean supportsAliasInSelect() {
		return true;
	}

	@Override
	public boolean supportsAliasInUpdate() {
		return true;
	}

	@Override
	public boolean supportsTableDropCascade() {
		return true;
	}

	@Override
	public boolean isDelimiterAfterBlock() {
		return true;
	}

	@NotNull
	@Override
	public DBDBinaryFormatter getNativeBinaryFormatter() {
		return BinaryFormatter.INSTANCE;
	}

	@Nullable
	@Override
	public String getDualTableName() {
		return "DUAL";
	}

	@NotNull
	@Override
	public String[] getNonTransactionKeywords() {
		return NON_TRANSACTIONAL_KEYWORDS;
	}

	@Override
	protected String getStoredProcedureCallInitialClause(DBSProcedure proc) {
		String schemaName = proc.getParentObject().getName();
		return "CALL " + schemaName + "." + proc.getName();
	}

	@Override
	public boolean isDisableScriptEscapeProcessing() {
		return preferenceStore.getBoolean(Constants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING);
	}

    @NotNull
    @Override
    public String[] getScriptDelimiters() {
        return super.getScriptDelimiters();
    }

	@Override
	public boolean isCRLFBroken() {
		return crlfBroken;
	}
}
