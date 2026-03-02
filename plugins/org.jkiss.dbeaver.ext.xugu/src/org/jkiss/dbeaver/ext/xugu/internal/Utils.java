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
package org.jkiss.dbeaver.ext.xugu.internal;

import com.xugu.parser.Parsing;
import com.xugu.parser.Parsing.TableType;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.UserRoleFlag;
import org.jkiss.dbeaver.ext.xugu.model.Package;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.ext.xugu.model.source.StatefulObject;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类，提供常用方法
 */
public class Utils {
	private static final Log LOG = Log.getLog(Utils.class);

	private static Map<String, Integer> typeMap = new HashMap<>();
	public static final String COLUMN_POSTFIX_PRIV = "_priv";



	static {
		typeMap.put("bit", java.sql.Types.BIT);
		typeMap.put("bool", java.sql.Types.BOOLEAN);
		typeMap.put("boolean", java.sql.Types.BOOLEAN);
		typeMap.put("tinyint", java.sql.Types.TINYINT);
		typeMap.put("smallint", java.sql.Types.SMALLINT);
		typeMap.put("mediumint", java.sql.Types.INTEGER);
		typeMap.put("int", java.sql.Types.INTEGER);
		typeMap.put("integer", java.sql.Types.INTEGER);
		typeMap.put("int24", java.sql.Types.INTEGER);
		typeMap.put("bigint", java.sql.Types.BIGINT);
		typeMap.put("real", java.sql.Types.DOUBLE);
		typeMap.put("float", java.sql.Types.REAL);
		typeMap.put("decimal", java.sql.Types.DECIMAL);
		typeMap.put("dec", java.sql.Types.DECIMAL);
		typeMap.put("numeric", java.sql.Types.DECIMAL);
		typeMap.put("double", java.sql.Types.DOUBLE);
		typeMap.put("double precision", java.sql.Types.DOUBLE);
		typeMap.put("char", java.sql.Types.CHAR);
		typeMap.put("varchar", java.sql.Types.VARCHAR);
		typeMap.put("date", java.sql.Types.DATE);
		typeMap.put("time", java.sql.Types.TIME);
		typeMap.put("year", java.sql.Types.DATE);
		typeMap.put("timestamp", java.sql.Types.TIMESTAMP);
		typeMap.put("datetime", java.sql.Types.TIMESTAMP);

		typeMap.put("tinyblob", java.sql.Types.BINARY);
		typeMap.put("blob", java.sql.Types.LONGVARBINARY);
		typeMap.put("mediumblob", java.sql.Types.LONGVARBINARY);
		typeMap.put("longblob", java.sql.Types.LONGVARBINARY);

		typeMap.put("tinytext", java.sql.Types.VARCHAR);
		typeMap.put("text", java.sql.Types.VARCHAR);
		typeMap.put("mediumtext", java.sql.Types.VARCHAR);
		typeMap.put("longtext", java.sql.Types.VARCHAR);

		typeMap.put(org.jkiss.dbeaver.ext.xugu.internal.Constants.TYPE_NAME_ENUM, java.sql.Types.CHAR);
		typeMap.put(org.jkiss.dbeaver.ext.xugu.internal.Constants.TYPE_NAME_SET, java.sql.Types.CHAR);
		typeMap.put("geometry", java.sql.Types.BINARY);
		typeMap.put("binary", java.sql.Types.BINARY);
		typeMap.put("varbinary", java.sql.Types.VARBINARY);
	}

	@SuppressWarnings("unchecked")
    public static <T> T getObjectAdapter(Object adapter, Class<T> objectType) {
        return Platform.getAdapterManager().getAdapter(adapter, objectType);
    }
	
	public static String formatWord(String word)
	{
		if (word == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(word.length());
		sb.append(Character.toUpperCase(word.charAt(0)));
		for (int i = 1; i < word.length(); i++) {
			char c = word.charAt(i);
			if ((c == 'i' || c == 'I') && sb.charAt(i - 1) == 'I') {
				sb.append('I');
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
	}
	
	public static int typeNameToValueType(String typeName) {
		Integer valueType = typeMap.get(typeName.toLowerCase(Locale.ENGLISH));
		return valueType == null ? java.sql.Types.OTHER : valueType;
	}

	/**
	 * 判断字符串是否可用
	 * 
	 * @param str 待判断字符串
	 * @return 判断结果
	 */
	public static boolean checkString(String str) {
		if (str == null || "".equals(str.trim())) {
			return false;
		}
		return true;
	}

	public static List<String> collectPrivilegeNames(ResultSet resultSet) {
		// Now collect all privileges columns
		try {
			List<String> privs = new ArrayList<>();
			ResultSetMetaData rsMetaData = resultSet.getMetaData();
			int colCount = rsMetaData.getColumnCount();
			for (int i = 0; i < colCount; i++) {
				String colName = rsMetaData.getColumnName(i + 1);
				if (colName.toLowerCase(Locale.ENGLISH).endsWith(COLUMN_POSTFIX_PRIV)) {
					privs.add(colName.substring(0, colName.length() - COLUMN_POSTFIX_PRIV.length()));
				}
			}
			return privs;
		} catch (SQLException e) {
			LOG.debug(e);
			return Collections.emptyList();
		}
	}

	public static Map<String, Boolean> collectPrivileges(List<String> privNames, ResultSet resultSet) {
		// Now collect all privileges columns
		Map<String, Boolean> privs = new TreeMap<>();
		for (String privName : privNames) {
			privs.put(privName, "Y".equals(JDBCUtils.safeGetString(resultSet, privName + COLUMN_POSTFIX_PRIV)));
		}
		return privs;
	}

	public static String determineCurrentDatabase(JDBCSession session) throws DBCException {
		// Get active schema
		try {
			try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DATABASE()")) {
				try (JDBCResultSet resultSet = dbStat.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getString(1);
					}
					return null;
				}
			}
		} catch (SQLException e) {
			throw new DBCException(e, session.getExecutionContext());
		}
	}

	public static String transformColumnAuthority(String authority) {
		String action = "";
		ResourceBundle  resourceBundle = ResourceBundle.getBundle("org.jkiss.dbeaver.ext.xugu.internal.authority",Locale.CHINA);
		if (authority != null) {
			// 处理动词
			final String authRead = resourceBundle.getString("read");
			final String authUpdate = resourceBundle.getString("update");
			if (authority.contains(authRead)) {
				action = "SELECT";
			} else if (authority.contains(authUpdate)) {
				action = "UPDATE";
			}
		}
		return action;
	}

	public static String transformAuthority(String authority, boolean isDatabase) {
		String action = "";
		String type = "";
		String any = "ANY";
		ResourceBundle  resourceBundle = ResourceBundle.getBundle("org.jkiss.dbeaver.ext.xugu.internal.authority",Locale.CHINA);
		if (authority != null) {
			// 处理动词
			final String authCreate = resourceBundle.getString("create");
			final String authAlter = resourceBundle.getString("alter");
			final String authDrop = resourceBundle.getString("drop");
			final String authSelect = resourceBundle.getString("select");
			final String authRead = resourceBundle.getString("read");
			final String authInsert = resourceBundle.getString("insert");
			final String authDelete = resourceBundle.getString("delete");
			final String authUpdate = resourceBundle.getString("update");
			final String authChange = resourceBundle.getString("change");
			final String authReference = resourceBundle.getString("reference");

			if (authority.contains(authCreate)) {
				action = "CREATE";
			} else if (authority.contains(authAlter)) {
				action = "ALTER";
			} else if (authority.contains(authDrop)) {
				action = "DROP";
			} else if (authority.contains(authSelect)) {
				action = "SELECT";
			} else if (authority.contains(authRead)) {
				action = "SELECT";
			} else if (authority.contains(authInsert)) {
				action = "INSERT";
			} else if (authority.contains(authDelete)) {
				action = "DELETE";
			} else if (authority.contains(authUpdate)) {
				action = "UPDATE";
			} else if (authority.contains(authChange)) {
				action = "UPDATE";
			} else if (authority.contains(authReference)) {
				action = "REFERENCES";
			}

			// 处理名词
			final String anthDatabase = resourceBundle.getString("database");
			final String authSchema = resourceBundle.getString("schema");
			final String authTable = resourceBundle.getString("table");
			final String authView = resourceBundle.getString("view");
			final String authSequence = resourceBundle.getString("sequence");
			final String authPackage = resourceBundle.getString("package");
			final String authProcedure = resourceBundle.getString("procedure");
			final String authTrigger = resourceBundle.getString("trigger");
			final String authColumn = resourceBundle.getString("column");
			final String authIndex = resourceBundle.getString("index");
			final String authUndoSegment = resourceBundle.getString("undosegment");
			final String authSynonym = resourceBundle.getString("synonym");
			final String authSnapshot = resourceBundle.getString("snapshot");
			final String authUser = resourceBundle.getString("user");
			final String authJob = resourceBundle.getString("job");
			final String authRole = resourceBundle.getString("role");
			final String authDir = resourceBundle.getString("dir");
			final String autuUdt = resourceBundle.getString("udt");
			final String authAny = resourceBundle.getString("any");

			if (authority.contains(anthDatabase)) {
				type = "DATABASE";
			} else if (authority.contains(authSchema)) {
				type = "SCHEMA";
			} else if (authority.contains(authTable)) {
				type = "TABLE";
			} else if (authority.contains(authView)) {
				type = "VIEW";
			} else if (authority.contains(authSequence)) {
				type = "SEQUENCE";
			} else if (authority.contains(authPackage)) {
				type = "PACKAGE";
			} else if (authority.contains(authProcedure)) {
				type = "PROCEDURE";
			} else if (authority.contains(authTrigger)) {
				type = "TRIGGER";
			} else if (authority.contains(authColumn)) {
				type = "COLUMN";
			} else if (authority.contains(authIndex)) {
				type = "INDEX";
			} else if (authority.contains(authUndoSegment)) {
				type = "UNDO SEGMENT";
			} else if (authority.contains(authSynonym)) {
				type = "SYNONYM";
			} else if (authority.contains(authSnapshot)) {
				type = "SNAPSHOT";
			} else if (authority.contains(authUser)) {
				type = "USER";
			} else if (authority.contains(authJob)) {
				type = "JOB";
			} else if (authority.contains(authRole)) {
				type = "ROLE";
			} else if (authority.contains(authDir)) {
				type = "DIR";
			} else if (authority.contains(autuUdt)) {
				type = "OBJECT";
			}
			if (!authority.contains(authAny)) {
				any = "";
			}
			if (isDatabase) {
				return action + " " + any + " " + type;
			} else {
				return action + " ON";
			}
		}
		return "";
	}

	public static String getDdl(DBRProgressMonitor monitor, String objectType, BaseTable object, DDLFormat ddlFormat,
			Map<String, Object> options) throws DBException {
		String objectFullName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
		monitor.beginTask("Load sources for " + objectType + " '" + objectFullName + "'...", 1);
		try (Connection conn = DBUtils.openUtilSession(monitor, object, "Get " + object.getName() + "DDL")) {
			String roleFlag = object.getDataSource().getRoleFlag();
			TableType tableType;
			
			if (UserRoleFlag.SYS.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.SYS;
			} else if (UserRoleFlag.DBA.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.DBA;
			} else {
				tableType = TableType.ALL;
			}
			
			Parsing parsing = new Parsing();
			return parsing.getObjectDDL(conn, object.getSchema().getName(), object.getName(), objectType, tableType);
		} catch (SQLException e) {
			throw new DBException("Close connection of DDL failed", e);
		}
	}

	public static int getDatabaseIdleTime(Connection conn) {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SHOW MAX_IDLE_TIME");
			if (rs.next()) {
				return rs.getInt(1);
			}
			return -1;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}

	public static void setCurrentSchema(JDBCSession session, String schema) throws SQLException {
		JDBCUtils.executeStatement(session,
				"SET CURRENT_SCHEMA TO " + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));
	}

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

	public static String normalizeSourceName(SourceObject object, boolean body) {
		try {
			String source = body ? ((DBPScriptObjectExt) object).getExtendedDefinitionText(null)
					: object.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS);
			if (source == null) {
				return null;
			}
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
					object.getSourceType() + (body ? "\\s+BODY" : "") + "\\s(\\s*)([\\w$\\.]+)[\\s\\(]+",
					java.util.regex.Pattern.CASE_INSENSITIVE);
			final Matcher matcher = pattern.matcher(source);
			if (matcher.find()) {
				String objectName = matcher.group(2);
				char separator = '.';
				if (objectName.indexOf(separator) == -1) {
					if (!objectName.equalsIgnoreCase(object.getName())) {
						object.setName(DBObjectNameCaseTransformer.transformObjectName(object, objectName));
						object.getDataSource().getContainer()
								.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
					}
					return source;
				}
			}
			return source.trim();
		} catch (DBException e) {
			LOG.error(e);
			return null;
		}
	}

	public static void addSchemaChangeActions(List<DBEPersistAction> actions, SourceObject object) {
		actions.add(0, new SQLDatabasePersistAction("Set target schema",
				"SET CURRENT_SCHEMA TO " + object.getSchema().getName(), DBEPersistAction.ActionType.INITIALIZER));
		if (object.getDataSource().getDefaultObject() != null) {
			if (object.getSchema() != object.getDataSource().getDefaultObject()) {
				actions.add(new SQLDatabasePersistAction("Set current schema",
						"SET CURRENT_SCHEMA TO " + object.getDataSource().getDefaultObject().getName(),
						DBEPersistAction.ActionType.FINALIZER));
			}
		}
	}

	/**
	 * xfc 修改了获取对象状态的sql和逻辑
	 * 
	 * @param monitor    数据库进程监视器
	 * @param object     有状态的对象
	 * @param objectType 对象类型
	 * @return 获取对象状态结果
	 * @throws DBCException 当发生 SQLException 时封装抛出
	 */
	public static boolean getObjectStatus(DBRProgressMonitor monitor, StatefulObject object, ObjectType objectType)
			throws DBCException {
		try (JDBCSession session = DBUtils.openMetaSession(monitor, object,
				"Refresh state of " + objectType.getTypeName() + " '" + object.getName() + "'")) {
			String validSql = "";
			switch (objectType) {
			case TABLE:
				validSql = "SELECT VALID FROM ALL_TABLES WHERE SCHEMA_ID=? AND TABLE_NAME=?";
				break;
			case VIEW:
				validSql = "SELECT VALID FROM ALL_VIEWS WHERE SCHEMA_ID=? AND VIEW_NAME=?";
				break;
			case PACKAGE:
				validSql = "SELECT VALID FROM ALL_PACKAGES WHERE SCHEMA_ID=? AND PACK_NAME=?";
				break;
			case PROCEDURE:
				validSql = "SELECT VALID FROM ALL_PROCEDURES WHERE SCHEMA_ID=? AND PROC_NAME=? AND RET_TYPE IS NULL";
				break;
			case FUNCTION:
				validSql = "SELECT VALID FROM ALL_PROCEDURES WHERE SCHEMA_ID=? AND PROC_NAME=? AND RET_TYPE IS NOT NULL";
				break;
			case SYNONYM:
				validSql = "SELECT VALID FROM ALL_SYNONYMS WHERE SCHEMA_ID=? AND SYNO_NAME=?";
				break;
			case INDEX:
				validSql = "SELECT VALID FROM ALL_INDEXES WHERE TABLE_ID=? AND INDEX_NAME=?";
				break;
			case SEQUENCE:
				validSql = "SELECT VALID FROM ALL_SEQUENCES WHERE SCHEMA_ID=? AND SEQ_NAME=?";
				break;
			case TRIGGER:
				validSql = "SELECT VALID FROM ALL_TRIGGERS WHERE SCHEMA_ID=? AND TRIG_NAME=?";
				break;
			case UDT:
				validSql = "SELECT VALID FROM ALL_TYPES WHERE SCHEMA_ID=? AND TYPE_NAME=?";
				break;
			default:
				validSql = "SELECT VALID FROM ALL_TABLES WHERE SCHEMA_ID=? AND TABLE_NAME=?";
				break;
			}
			try (JDBCPreparedStatement dbStat = session.prepareStatement(validSql)) {
				// 在数据库中 obj_type 字段为 int 类型
				dbStat.setLong(1,
						objectType == ObjectType.INDEX ? object.getSchema().getId() : object.getSchema().getId());
				dbStat.setString(2, DBObjectNameCaseTransformer.transformObjectName(object, object.getName()));
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						return dbResult.getBoolean(1);
					} else {
						LOG.warn(objectType.getTypeName() + " '" + object.getName()
								+ "' not found in system dictionary");
						return false;
					}
				}
			}
		} catch (SQLException e) {
			throw new DBCException("Query object valid status failed", e);
		}
	}

	public static String insertCreateReplace(SourceObject object, boolean body, String source) {
		String sourceType = object.getSourceType().name();
		if (body) {
			sourceType += " BODY";
		}
		Pattern srcPattern = Pattern.compile("^(" + sourceType + ")\\s+(\"{0,1}\\w+\"{0,1})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = srcPattern.matcher(source);
		if (matcher.find()) {
			return "CREATE OR REPLACE " + matcher.group(1) + " " + DBUtils.getQuotedIdentifier(object.getSchema()) + "."
					+ matcher.group(2) + source.substring(matcher.end());
		}
		return source;
	}

	public static String getObjectList(DataSource source, DBRProgressMonitor monitor, String schemaName, String type,
			String tableName) {
		try {
			Collection<Table> tableList = null;
			Collection<View> viewList = null;
			Collection<Sequence> seqList = null;
			Collection<Package> pacList = null;
			Collection<ProcedureStandalone> procList = null;
			Collection<? extends DBSTrigger> triList = null;
			List<TableColumn> colList = null;
			switch (type) {
			case "TABLE":
				tableList = source.schemaCache.getCachedObject(schemaName).getTables(monitor);
				break;
			case "VIEW":
				viewList = source.schemaCache.getCachedObject(schemaName).getViews(monitor);
				break;
			case "SEQUENCE":
				seqList = source.schemaCache.getCachedObject(schemaName).getSequences(monitor);
				break;
			case "PACKAGE":
				pacList = source.schemaCache.getCachedObject(schemaName).getPackages(monitor);
				break;
			case "PROCEDURE":
				procList = source.schemaCache.getCachedObject(schemaName).getProcedures(monitor);
				break;
			case "TRIGGER":
				triList = source.schemaCache.getCachedObject(schemaName).getTable(monitor, tableName)
						.getTriggers(monitor);
				break;
			case "COLUMN":
				Schema schema = source.getSchema(monitor, schemaName);
				Table table = source.schemaCache.getCachedObject(schemaName).getTable(monitor, tableName);
				colList = source.schemaCache.getCachedObject(schemaName).tableCache.getChildren(monitor, schema, table);
				break;
			default:
				break;
			}

			final String typeTable = "TABLE";
			if (tableList != null && typeTable.equals(type)) {
				String res = "";
				Iterator<Table> it = tableList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
			if (viewList != null) {
				String res = "";
				Iterator<View> it = viewList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
			if (seqList != null) {
				String res = "";
				Iterator<Sequence> it = seqList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
			if (pacList != null) {
				String res = "";
				Iterator<Package> it = pacList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
			if (procList != null) {
				String res = "";
				Iterator<ProcedureStandalone> it = procList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
			if (triList != null && triList.size() > 0) {
				String res = "";
				Iterator<? extends DBSTrigger> it = triList.iterator();
				Trigger trigger = (Trigger) it.next();
				while (it.hasNext()) {
					res += trigger.getTable().getName() + "." + trigger.getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
			if (colList != null && colList.size() > 0) {
				String res = "";
				Iterator<TableColumn> it = colList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
				}
				if (res.length() > 0) {
					res = res.substring(0, res.length() - 1);
				}
				return res;
			}
		} catch (DBException e) {
			e.printStackTrace();
		}
		return "";
	}
	public static boolean isXuguObject(Object object) {
		if (object == null) {
			return false;
		}
		String className = object.getClass().getName();
		return className.equals(Constants.XUGU_DBOBJECT_CLASS);
	}
	public static Object extractPGObjectValue(Object xuguObject) {
		if (xuguObject == null) {
			return null;
		}
		if (!isXuguObject(xuguObject)) {
			return xuguObject;
		}
		try {
			return xuguObject.getClass().getMethod("getValue").invoke(xuguObject);
		} catch (Exception e) {
			LOG.debug("Can't extract value from " + xuguObject.getClass().getName(), e);
		}
		return null;
	}

    public static DataType findDataType(DBCSession session, DataSource dataSource, DBSTypedObject type) throws DBCException {
        if (type instanceof DataType) {
            return (DataType) type;
        } else {
            DBRProgressMonitor monitor = session.getProgressMonitor();
            if (type instanceof JDBCColumnMetaData) {
                try {
                    DBCEntityMetaData entityMetaData = ((DBCAttributeMetaData) type).getEntityMetaData();
                    if (entityMetaData != null) {
                        DBSEntity docEntity = DBUtils.getEntityFromMetaData(monitor, session.getExecutionContext(), entityMetaData);
                        if (docEntity != null) {
                            DBSEntityAttribute attribute = docEntity.getAttribute(monitor, ((DBCAttributeMetaData) type).getName());
                            if (attribute instanceof DBSTypedObjectEx) {
                                DBSDataType dataType = ((DBSTypedObjectEx) attribute).getDataType();
                                if (dataType instanceof DataType) {
                                    return (DataType) dataType;
                                }
                            }
                        }
                    } else {
                        String typeName = type.getTypeName();
                        DataType dataType = dataSource.dataTypeCache.getCachedObject(typeName);
                        if (dataType != null) {
                            return dataType;
                        }
                    }
                } catch (DBException e) {
                    throw new DBCException("Error extracting column " + type + " data type", e);
                }
            }

            String typeName = type.getTypeName();
            return (DataType) dataSource.getLocalDataType(typeName);
        }
    }
}
