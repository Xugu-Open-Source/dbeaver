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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.UserRoleFlag;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Statement;

import com.xugu.parser.DatabaseParsing;
import com.xugu.parser.Parsing;
import com.xugu.parser.Parsing.TableType;
import com.xugu.permission.LoadPermission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * 用户信息类，包含名称、用户权限等具体信息
 */
public class User extends BaseGlobalObject implements DBAUser, DBPRefreshableObject, DBPSaveableObject, DBPScriptObject {
	private static final Log log = Log.getLog(User.class);

	private Vector<String> authorityKey;
	private Vector<String> authorityValue;

	private UserAuthority authority;
	DBRProgressMonitor monitor;
	private int dbId;
	private int userId;
	private String userName;
	private boolean role;
	private String password;
	private Timestamp startTime;
	private String untilTime;
	private boolean locked;
	private boolean expired;
	private Timestamp passSetTime;
	private Timestamp passSetPeriod;
	private String alias;
	private boolean sys;
	private String trustIp;
	private int tempSpaceQuota;
	private int cursorAuota;
	private int sessionQuota;
	private int ioQuota;
	private Timestamp createTime;
	private Timestamp lastModiTime;
	private String roleList;
	private Collection<UserAuthority> userAuthorities;
	private String schemaList;
	private Database parent;
	public static List<String> roleNames = new ArrayList<String>();
	
	public static List<String> getRoleNameList(){
		return roleNames;
	}
	
	public User(DataSource dataSource, String userName, boolean persisted) {
		super(dataSource, persisted);
		this.userName = userName;
		this.parent = dataSource.getDatabase();
	}
	
	public User(DataSource dataSource,DBRProgressMonitor moniter, boolean persisted) {
		super(dataSource, persisted);
		this.monitor = monitor;
		this.parent = dataSource.getDatabase();
	}
	
	
	public User(DataSource dataSource, ResultSet resultSet, DBRProgressMonitor monitor, boolean persisted) {
		super(dataSource, persisted);
		this.monitor = monitor;
		this.parent = dataSource.getDatabase();
		if (resultSet != null) {
			this.dbId = JDBCUtils.safeGetInt(resultSet, "DB_ID");
			this.userId = JDBCUtils.safeGetInt(resultSet, "USER_ID");
			this.userName = JDBCUtils.safeGetString(resultSet, "USER_NAME");
			this.role = JDBCUtils.safeGetBoolean(resultSet, "IS_ROLE");
			this.password = JDBCUtils.safeGetString(resultSet, "PASSWORD");
			this.startTime = JDBCUtils.safeGetTimestamp(resultSet, "START_TIME");

			this.untilTime = JDBCUtils.safeGetString(resultSet, "UNTIL_TIME");
			this.locked = JDBCUtils.safeGetBoolean(resultSet, "LOCKED");
			this.expired = JDBCUtils.safeGetBoolean(resultSet, "EXPIRED");

			this.passSetTime = JDBCUtils.safeGetTimestamp(resultSet, "PASS_SET_TIME");
			this.passSetPeriod = JDBCUtils.safeGetTimestamp(resultSet, "PASS_SET_PERIOD");
			this.alias = JDBCUtils.safeGetString(resultSet, "ALIAS");
			this.sys = JDBCUtils.safeGetBoolean(resultSet, "IS_SYS");
			this.trustIp = JDBCUtils.safeGetString(resultSet, "TRUST_IP");
			this.tempSpaceQuota = JDBCUtils.safeGetInt(resultSet, "TEMP_SPACE_QUOTA");
			this.cursorAuota = JDBCUtils.safeGetInt(resultSet, "CURSOR_QUOTA");
			this.sessionQuota = JDBCUtils.safeGetInt(resultSet, "SESSION_QUOTA");
			this.ioQuota = JDBCUtils.safeGetInt(resultSet, "IO_QUOTA");
			this.createTime = JDBCUtils.safeGetTimestamp(resultSet, "CREATE_TIME");
			this.lastModiTime = JDBCUtils.safeGetTimestamp(resultSet, "LAST_MODI_TIME");
		}

		// 加载 roleList 和 schemaList
		// 获取SYSDBA连接,用来获取当前用户所包含的角色信息
		try (Connection tempConn = resultSet.getStatement().getConnection();
				Statement stmt = tempConn.createStatement()) {
			String sql = "SELECT USER_NAME FROM ";
	 
			sql += "SYS_USERS  SU WHERE SU.USER_ID IN(SELECT ROLE_ID FROM SYS_ROLE_MEMBERS SRM WHERE SRM.USER_ID=";
			sql += this.userId;
			sql += " AND SRM.DB_ID = ";
			sql += this.dbId;
			sql += " ) AND SU.DB_ID=";
			sql += this.dbId;
			sql += " AND IS_ROLE=TRUE";
			ResultSet rs = stmt.executeQuery(sql);
			// 获取当前用户所含角色信息
			String text = "";
			while (rs.next()) {
				String role = rs.getString(1);
				text += role + ",";
			}
			if (!text.isEmpty()) {
				text = text.substring(0, text.length() - 1);
			}
			this.setRoleList(text);

			// 获取全部角色信息,并加入全部角色列表中
			Collection<Role> allRoleList = dataSource.getRoles(monitor);
			if (allRoleList != null && allRoleList.size() != 0) {
				roleNames.clear();
				Iterator<Role> it = allRoleList.iterator();
				while (it.hasNext()) {
					roleNames.add(it.next().getName());
				}
			}

			// 获取全部模式信息
			Collection<Schema> schemaList = dataSource.getSchemas(monitor);
			if (schemaList != null && schemaList.size() != 0) {
				Iterator<Schema> it = schemaList.iterator();
				String text2 = "";
				while (it.hasNext()) {
					Schema tempSchema = it.next();
					// 构造schemalist
					text2 += tempSchema.getName() + ",";
				}
				if (!"".equals(text2)) {
					text2 = text2.substring(0, text2.length() - 1);
				}
				this.setSchemaList(text2);
			}
		} catch (DBException | SQLException e) {
			e.printStackTrace();
		}
		if (resultSet != null) {
			reloadAuthrities();
		}
	}

	public void reloadAuthrities() {
		// 加载权限
		Connection conn;
		try {
			conn = this.getDataSource().getDefaultInstance().getDefaultContext(true).getConnection(new LoggingProgressMonitor());
		} catch (SQLException e) {
			throw new RuntimeException("获取用户权限查询连接失败", e);
		}
		Vector<Object> authorities = new LoadPermission().loadPermission(conn, this.userName, 0);
		userAuthorities = new ArrayList<>();
		Iterator<Object> it = authorities.iterator();
		while (it.hasNext()) {
			String temp = it.next().toString();
			// 对象级权限
			if (temp.indexOf("\"") != -1) {
				String targetName = temp.substring(temp.indexOf("\""));
				UserAuthority one = new UserAuthority(this, temp, targetName, false, expired);
				userAuthorities.add(one);
			}
			// 库级权限
			else {
				UserAuthority one = new UserAuthority(this, temp, null, true, expired);
				userAuthorities.add(one);
			}
		}
	}

	public static Log getLog() {
		return log;
	}

	public int getDbId() {
		return dbId;
	}

	public int getUserId() {
		return userId;
	}

	@Override
	@Property(viewable = true, order = 0)
	public String getName() {
		return userName;
	}

	public void setName(String name) {
		this.userName = name;
	}

	@Property(viewable = true, order = 1)
	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isRole() {
		return role;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	@Property(viewable = true, editable = true, updatable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public String getUntilTime() {
		return untilTime;
	}

	public void setUntilTime(String time) {
		this.untilTime = time;
	}

	@Property(viewable = true, editable = true, updatable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 4)
	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}

	@Property(viewable = true, editable = true, updatable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 5)
	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	@Property(viewable = true, editable = true)
	public String getRoleList() {
		return this.roleList;
	}

	public void setRoleList(String list) {
		this.roleList = list;
	}

//	@Property(viewable = true, editable = true)
//	public static String getAllRoleList() {
//		return  allRoleList;
//	}
//
//	public void setAllRoleList(String list) {
//		this.allRoleList = list;
//	}

	@Property(viewable = true, editable = true)
	public String getSchemaList() {
		return this.schemaList;
	}

	public void setSchemaList(String list) {
		this.schemaList = list;
	}

	public Timestamp getPassSetTime() {
		return passSetTime;
	}

	public Timestamp getPassSetPeriod() {
		return passSetPeriod;
	}

	public String getAlias() {
		return alias;
	}

	public boolean isSys() {
		return sys;
	}

	public String getTrustIp() {
		return trustIp;
	}

	public int getTempSpaceQuota() {
		return tempSpaceQuota;
	}

	public int getCursorQuota() {
		return cursorAuota;
	}

	public int getSessionQuota() {
		return sessionQuota;
	}

	public int getIoQuota() {
		return ioQuota;
	}

	public Timestamp getCreateTime() {
		return createTime;
	}

	public Timestamp getLastModiTime() {
		return lastModiTime;
	}

	public Vector<String> getAuthorityKey() {
		return authorityKey;
	}

	public Vector<String> getAuthorityValue() {
		return authorityValue;
	}

	public UserAuthority getAuthority() {
		return authority;
	}

	public Database getParent() {
		return parent;
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		User user = this.getDataSource().userCache.refreshObject(monitor, this.getDataSource(), this);
		user.reloadAuthrities();
		return user;
	}

	public Collection<UserAuthority> getUserAuthorities() {
		return userAuthorities;
	}

	public Collection<UserAuthority> getUserDatabaseAuthorities() {
		Collection<UserAuthority> res = new ArrayList<>();
		if(userAuthorities==null) {
			return null;
		} 
		Iterator<UserAuthority> it = userAuthorities.iterator();
		while (it.hasNext()) {
			UserAuthority authority = it.next();
			if (authority.isDatabase) {
				res.add(authority);
			}
		}
		return res;
	}

	public Collection<UserAuthority> getUserObjectAuthorities() {
		Collection<UserAuthority> res = new ArrayList<>();
		if(userAuthorities==null) {
			return null;
		}
		Iterator<UserAuthority> it = userAuthorities.iterator();
		while (it.hasNext()) {
			UserAuthority authority = it.next();
			final boolean isNotContainColumn = !authority.getName().contains("列");
			if (!authority.isDatabase && isNotContainColumn) {
				res.add(authority);
			}
		}
		return res;
	}

	public Collection<UserAuthority> getUserSubObjectAuthorities() {
		Collection<UserAuthority> res = new ArrayList<>();
		if(userAuthorities==null) {
			return null;
		}
		Iterator<UserAuthority> it = userAuthorities.iterator();
		while (it.hasNext()) {
			UserAuthority authority = it.next();
			final boolean isContainColumn = authority.getName().contains("列");
			if (!authority.isDatabase && isContainColumn) {
				res.add(authority);
			}
		}
		return res;
	}

	public void addUserAuthority(UserAuthority authority) {
		userAuthorities.add(authority);
	}

	public void removeAuthority(UserAuthority authority) {
		userAuthorities.remove(authority);
	}

	public String getObjectList(String schemaName, String type, String tableName) {
		try {
			Collection<Table> tableList = null;
			Collection<View> viewList = null;
			Collection<Sequence> seqList = null;
			Collection<Package> pacList = null;
			Collection<ProcedureStandalone> procList = null;
			Collection<Trigger> triList = null;
			List<TableColumn> colList = null;
			switch (type) {
			case "TABLE":
				tableList = this.getDataSource().schemaCache.getCachedObject(schemaName).getTables(monitor);
				break;
			case "VIEW":
				viewList = this.getDataSource().schemaCache.getCachedObject(schemaName).getViews(monitor);
				break;
			case "SEQUENCE":
				seqList = this.getDataSource().schemaCache.getCachedObject(schemaName).getSequences(monitor);
				break;
			case "PACKAGE":
				pacList = this.getDataSource().schemaCache.getCachedObject(schemaName).getPackages(monitor);
				break;
			case "PROCEDURE":
				procList = this.getDataSource().schemaCache.getCachedObject(schemaName).getProcedures(monitor);
				break;
			case "TRIGGER":
				triList = this.getDataSource().schemaCache.getCachedObject(schemaName).getTriggers(monitor);
				break;
			case "COLUMN":
				Schema schema = this.getDataSource().getSchema(monitor, schemaName);
				Table table = this.getDataSource().schemaCache.getCachedObject(schemaName).getTable(monitor, tableName);
				colList = this.getDataSource().schemaCache.getCachedObject(schemaName).tableCache.getChildren(monitor,
						schema, table);
				break;
			default:
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
				Iterator<Trigger> it = triList.iterator();
				while (it.hasNext()) {
					res += it.next().getName() + ",";
					;
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

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		String objectFullName = DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL);
		monitor.beginTask("Load sources for user '" + objectFullName + "'...", 1);
		try (Connection conn = DBUtils.openUtilSession(monitor, this, "Get " + this.userName + "DDL")) {
			String roleFlag = getDataSource().getRoleFlag();
			TableType tableType;

			if (UserRoleFlag.SYS.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.SYS;
			} else if (UserRoleFlag.DBA.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.DBA;
			} else {
				tableType = TableType.ALL;
			}

			Parsing parsing = new Parsing();
			return parsing.loadTheUserDDL(conn, getDataSource().getDatabase().getName(), getName(), tableType);
		} catch (SQLException e) {
			throw new DBException("Close connection of DDL failed", e);
		}
	}
}
