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
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.data.content.BinaryFormatter;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
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
		try(Statement statement = metaData.getConnection().createStatement()){
			ResultSet resultSet = statement.executeQuery("SELECT DISTINCT  name from ALL_METHODS order by name ASC");
			ArrayList<String> methodNames = new ArrayList<>();
			while (resultSet.next()) {
				String methodName = resultSet.getString("name");
				methodNames.add(methodName);
			}
			addFunctions(methodNames);
		} catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
