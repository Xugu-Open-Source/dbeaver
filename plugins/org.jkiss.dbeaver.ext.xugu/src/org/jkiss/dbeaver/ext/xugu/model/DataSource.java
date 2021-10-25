/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.AsyncServerOutputReader;
import org.jkiss.dbeaver.model.impl.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.sql.SQLState;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.BeanUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import cn.hutool.core.util.EscapeUtil;

import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.SchedulerJobCache;
import org.jkiss.dbeaver.ext.xugu.model.Schema.SynonymCache;
import org.jkiss.dbeaver.ext.xugu.model.plan.PlanAnalyser;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据源类，包含连接信息以及模式级别的对象缓存（模式、角色、用户、表空间、数据类型） 负责创建连接、初始化上下文等
 */
public class DataSource extends JDBCDataSource implements DBCQueryPlanner, IAdaptable, javax.sql.DataSource {
	private static final Log log = Log.getLog(DataSource.class);
	
	
	
	private static final String DBA = "DBA";
	private static final String SYSDBA = "SYSDBA";
	private static final String M = "M";
	

	public enum UserLoginRole {
		/**
		 * 用户登录角色
		 */
		SYSDBA, DBA, NORMAL
	}

	public enum UserRoleFlag {
		/**
		 * 用户角色标志
		 */
		SYS, DBA, ALL
	}

	private int defTime;
	private JDBCSession metaSession;
	private JDBCSession utilSession;

	final public SchemaCache schemaCache = new SchemaCache();
	final public DatabaseCache databaseCache = new DatabaseCache();
	final DataTypeCache dataTypeCache = new DataTypeCache();
	final public SchedulerJobCache schedulerJobCache = new SchedulerJobCache();
	final public SynonymCache synonymCache = new SynonymCache();

	private final TablespaceCache tablespaceCache = new TablespaceCache();
	final public UserCache userCache = new UserCache();
	final public RoleCache roleCache = new RoleCache();

	private static final ExecutorService THREAD_POOL_EXECUTOR = Executors.newCachedThreadPool();

	private OutputReader outputReader;
	private Schema publicSchema;
	private String activeSchemaName;
	private boolean isAdmin;
	private boolean isAdminVisible;
	private boolean useRuleHint;
	private Database database;
	/**
	 * userRole 角色属性，用于在查询时设置表名的前缀
	 */
	private String userRole;
	private String roleFlag;

	private List<Charset> charsets;

	private final Map<String, Boolean> availableViews = new HashMap<>();
	
	private String  roleString;
	private String 	userString;
	private Driver driver;
	
	public DataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
		super(monitor, container, new SqlDialect());
		DBPConnectionConfiguration config = container.getConnectionConfiguration();
		try {
			driver = (Driver) container.getDriver().getDriverInstance(monitor);
		} catch (DBException e) {
			throw new DBException("注册驱动失败", e);
		}
		// xfc 从连接信息中获取 userRole
		this.userRole = config.getProviderProperty(Constants.PROP_INTERNAL_LOGON);
		if (UserLoginRole.SYSDBA.name().equals(this.userRole)) {
			this.roleFlag = UserRoleFlag.SYS.name();
		} else if (UserLoginRole.DBA.name().equals(this.userRole)) {
			this.roleFlag = UserRoleFlag.DBA.name();
		} else {
			this.roleFlag = UserRoleFlag.ALL.name();
		}
		this.outputReader = new OutputReader();
	}
	
	@Override
	public Object getDataSourceFeature(String featureId) {
		switch (featureId) {
		case DBConstants.FEATURE_MAX_STRING_LENGTH:
			return 4000;
		default:
			return super.getDataSourceFeature(featureId);
		}
	}

	public Database getDatabase() {
		if (database == null) {
			try {
				DBPConnectionConfiguration config = this.getContainer().getConnectionConfiguration();
				this.database = this.databaseCache.getObject(new LoggingProgressMonitor(), this, config.getDatabaseName());
			} catch (DBException e) {
				throw new RuntimeException(e);
			}
		}
		return database;
	}

	public void setDatabase(Database database) {
		this.database = database;
	}

	/**
	 * 基本操作为打开连接，但是当第一次执行时，会加载一个守护进程以检查连接状态
	 */
	@Override
	protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context,
			@NotNull String purpose) throws DBCException {
		Connection connection = super.openConnection(monitor, context, purpose);

		// 校验用户权限
		try {
			Statement statement = connection.createStatement();
			String sqlString  = "select user_id,role_id,authority "
					+ "from dba_role_members m,dba_acls a "
					+ "where m.role_id=a.grantee_id and  m.user_id=current_userid "
					+ "union select db_id,grantee_id,authority "
					+ "from dba_acls where db_id=current_db_id and grantee_id=current_userid";
			if(DBA.equals(roleString)) {
			  statement.executeQuery(sqlString);
			}
			if(SYSDBA.equals(roleString)) {
				statement.executeQuery(sqlString);
			}
		} catch (SQLException e) {
			if(DBA.equals(roleString)) {
				throw new DBCException("No DBA authority", e);
			}else {
				throw new DBCException("No SYSDBA authority", e);
			}
		}

		// 处理连接保活
		Path configPath = Paths.get(OemConfig.OEM_NAME_EN.toLowerCase() + ".properties");
		String enableConnectKeepAliveKey = "enable-connect-keep-alive";
		String enableConnectKeepAliveValue = "true";
		String connectKeepAliveMillisecondsKey = "connect-keep-alive-milliseconds";
		String connectKeepAliveMillisecondsValue = "5000";
		
		// 检测配置文件是否存在，若不存在，创建新的默认配置文件
		if (!Files.exists(configPath)) {
			try {
				Files.createFile(configPath);
				Properties defaultConfigProperties = new Properties();
				try (OutputStream os = Files.newOutputStream(configPath)) {
					defaultConfigProperties.setProperty(enableConnectKeepAliveKey, enableConnectKeepAliveValue);
					defaultConfigProperties.setProperty(connectKeepAliveMillisecondsKey, connectKeepAliveMillisecondsValue);
					defaultConfigProperties.store(os, null);
					log.debug(OemConfig.OEM_NAME_EN + "配置文件默认配置保存成功：" + configPath.toAbsolutePath());
				} catch (IOException e) {
					throw new IllegalStateException(OemConfig.OEM_NAME_EN + "配置文件默认配置保存失败：" + configPath.toAbsolutePath(), e);
				}
			} catch (IOException e) {
				throw new IllegalStateException(OemConfig.OEM_NAME_EN + "配置文件创建失败：" + configPath.toAbsolutePath(), e);
			}
		}
		
		// 从配置文件加载配置
		Properties configProperties = new Properties();
		try (InputStream is = Files.newInputStream(configPath)) {
			configProperties.load(is);
			log.debug(OemConfig.OEM_NAME_EN + "配置文件读取成功：" + configPath.toAbsolutePath());
		} catch (IOException e) {
			throw new IllegalStateException(OemConfig.OEM_NAME_EN + "配置文件读取失败：" + configPath.toAbsolutePath(), e);
		}
		String enableConnectKeepAlive = configProperties.getProperty(enableConnectKeepAliveKey, enableConnectKeepAliveValue);
		String connectKeepAliveMilliseconds = configProperties.getProperty(connectKeepAliveMillisecondsKey, connectKeepAliveMillisecondsValue);
		if (Boolean.parseBoolean(enableConnectKeepAlive)) {
			// 创建连接保活线程
			long keepAliveTime = Long.parseLong(connectKeepAliveMilliseconds);
			THREAD_POOL_EXECUTOR.execute(() -> {
				try {
					while (!connection.isClosed()) {
						Statement stmt = connection.createStatement();
						stmt.executeQuery("SELECT 1 FROM DUAL");
						stmt.close();
						Thread.sleep(keepAliveTime);
					}
					log.debug("连接保活线程已销毁，执行环境：" + context.getContextName());
				} catch (SQLException e) {
					log.debug("连接保活进程SQL异常", e);
					DBPDataSourceContainer container = context.getDataSource().getContainer();
					try {
						container.reconnect(monitor);
					} catch (DBException e1) {
						log.debug("重新连接失败", e1);
					}
				} catch (InterruptedException e) {
					log.debug("连接保活进程已被中断", e);
					Thread.currentThread().interrupt();
				}
			});
			log.debug("连接保活线程已创建，保活间隔 " + keepAliveTime / 1000 + " 秒，执行环境：" + context.getContextName());
		} else {
			log.debug("未开启连接保活功能，执行环境：" + context.getContextName());
		}
		return connection;
	}

	@Override
	protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
		return new ExecutionContext(instance, type);
	}

	/**
	 * 初始化上下文
	 */
	@Override
	protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context,
			JDBCExecutionContext initFrom) throws DBException {
		if (outputReader == null) {
			outputReader = new OutputReader();
		}

		if (initFrom != null) {
			((ExecutionContext) context).setCurrentSchema(monitor, ((ExecutionContext) initFrom).getDefaultSchema());
		} else {
			((ExecutionContext) context).refreshDefaults(monitor, true);
		}

		try (JDBCSession session = context.openSession(monitor, DBCExecutionPurpose.META, "Set connection parameters");
				Statement dbStat = session.createStatement();
				ResultSet dbResult = dbStat.executeQuery("SHOW CHARSETS")) {
			// 读取字符集和排序集
			charsets = new ArrayList<>();
			while (dbResult.next()) {
				Charset charset = new Charset(this, session, JDBCUtils.safeGetString(dbResult, "CHARSET_NAME"));
				charsets.add(charset);
			}
			Collections.sort(charsets, DBUtils.<Charset>nameComparator());
		} catch (SQLException e) {
			throw new DBException("Read character sets and collations failed", e);
		}
	}

	@Override
	protected String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo)  {
		return connectionInfo.getUserName();
	}

	@Override
	public ErrorType discoverErrorType(@NotNull Throwable error) {
		Throwable rootCause = GeneralUtils.getRootCause(error);
		if (rootCause instanceof SQLException
				&& ((SQLException) rootCause).getErrorCode() == Constants.EC_FEATURE_NOT_SUPPORTED) {
			return ErrorType.FEATURE_UNSUPPORTED;
		}
		return super.discoverErrorType(error);
	}

	@Override
	protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver,
			JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo)
			throws DBCException {
		Map<String, String> connectionsProps = new HashMap<String, String>(16);
		if (CommonUtils.toBoolean(connectionInfo.getProviderProperty(Constants.OS_AUTH_PROP))) {
			connectionsProps.put("v$session.osuser", System.getProperty(StandardConstants.ENV_USER_NAME));
		}
		return connectionsProps;
	}

	public String getRoleFlag() {
		return this.roleFlag;
	}

	@Association
	public Collection<Database> getDatabases(DBRProgressMonitor monitor) throws DBException {
		return databaseCache.getAllObjects(monitor, this);
	}

	@Association
	public Database getDatabase(DBRProgressMonitor monitor, String name) throws DBException {
		return databaseCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<Schema> getSchemas(DBRProgressMonitor monitor) throws DBException {
		return schemaCache.getAllObjects(monitor, this.getDatabase());
	}

	@Association
	public Schema getSchema(DBRProgressMonitor monitor, String name) throws DBException {
		return schemaCache.getObject(monitor, this.getDatabase(), name);
	}

	@Association
	public Collection<Tablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
		return getTablespaceCache().getAllObjects(monitor, this);
	}

	@Association
	public Collection<User> getUsers(DBRProgressMonitor monitor) throws DBException {
		Collection<User> allusers = userCache.getAllObjects(monitor, this);
		return allusers;
	}

	@Association
	public User getUser(DBRProgressMonitor monitor, String name) throws DBException {
		return userCache.getObject(monitor, this, name);
	}

	@Association
	public Collection<Role> getRoles(DBRProgressMonitor monitor) throws DBException {
		return roleCache.getAllObjects(monitor, this);
	}

	@Association
	public Collection<PublicSynonym> getPublicSynonyms(DBRProgressMonitor monitor) throws DBException {
		return synonymCache.getAllObjects(monitor, this);
	}

	@Override
	public void initialize(@NotNull DBRProgressMonitor monitor) throws DBException {
		super.initialize(monitor);
		DBPConnectionConfiguration connectionInfo = getContainer().getConnectionConfiguration();
		String useRuleHintProp = connectionInfo.getProviderProperty(Constants.PROP_USE_RULE_HINT);
		if (useRuleHintProp != null) {
			useRuleHint = CommonUtils.getBoolean(useRuleHintProp, false);
		}
		{
			JDBCSession session = DBUtils.openMetaSession(monitor, this, "Check meta connection");
			this.metaSession = session;
			JDBCSession session2 = DBUtils.openUtilSession(monitor, this, "Check util connection");
			this.utilSession = session2;
		}

		// 真正进行数据类型缓存
		List<DataType> dtList = new ArrayList<>();
		for (Map.Entry<String, DataType.TypeDesc> predefinedType : DataType.PREDEFINED_TYPES.entrySet()) {
			DataType dataType = new DataType(this, predefinedType.getKey(), true);
			dtList.add(dataType);
		}
		this.dataTypeCache.setCache(dtList);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		super.refreshObject(monitor);

		this.databaseCache.clearCache();
		this.schemaCache.clearCache();
		this.tablespaceCache.clearCache();
		this.userCache.clearCache();
		if (UserRoleFlag.SYS.name().equals(this.roleFlag)) {
			this.roleCache.clearCache();
		}
		this.schedulerJobCache.clearCache();
		this.synonymCache.clearCache();

		this.initialize(monitor);
		return this;
	}

	@Override
	public Collection<Schema> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getSchemas(monitor);
	}

	@Override
	public Schema getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		return getSchema(monitor, childName);
	}

	@Override
	public Class<? extends Schema> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
		return Schema.class;
	}

	@Override
	public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
		// TODO 缓存结构
	}

	public boolean supportsDefaultChange() {
		return true;
	}

	@Nullable
	public Schema getDefaultObject() {
		return getActiveSchemaName() == null ? null : schemaCache.getCachedObject(getActiveSchemaName());
	}

	/**
	 * 设为默认对象
	 * 
	 * @param monitor 进程监视器
	 * @param object  数据库对象
	 * @throws DBException 数据库异常
	 */
	public void setDefaultObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) throws DBException {
		final Schema oldSelectedEntity = getDefaultObject();
		if (!(object instanceof Schema)) {
			throw new IllegalArgumentException("无效的对象类型：" + object);
		}
		for (JDBCExecutionContext context : getDefaultInstance().getAllContexts()) {
			setCurrentSchema(monitor, context, (Schema) object);
		}

		// 发送通知
		if (oldSelectedEntity != null) {
			DBUtils.fireObjectSelect(oldSelectedEntity, false);
		}
		if (this.getActiveSchemaName() != null) {
			DBUtils.fireObjectSelect(object, true);
		}
	}

	public boolean refreshDefaultObject(@NotNull DBCSession session) throws DBException {
		try {
			final String currentSchema = Utils.getCurrentSchema((JDBCSession) session, this.userRole);
			if (currentSchema != null && !CommonUtils.equalObjects(currentSchema, getActiveSchemaName())) {
				final Schema newSchema = schemaCache.getCachedObject(currentSchema);
				if (newSchema != null) {
					setDefaultObject(session.getProgressMonitor(), newSchema);
					return true;
				}
			}
			return false;
		} catch (SQLException e) {
			throw new DBException(e, this);
		}
	}

	private void setCurrentSchema(DBRProgressMonitor monitor, JDBCExecutionContext executionContext, Schema object)
			throws DBCException {
		if (object == null) {
			log.debug("当前模式为空");
			return;
		}
		try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.UTIL,
				"Set active schema")) {
			Utils.setCurrentSchema(session, object.getName());
		} catch (SQLException e) {
			throw new DBCException(e, executionContext);
		}
	}

	@Nullable
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == DBSStructureAssistant.class) {
			return adapter.cast(new StructureAssistant(this));
		} else if (adapter == DBCServerOutputReader.class) {
			return adapter.cast(outputReader);
		}
		return super.getAdapter(adapter);
	}

	@Override
	public void cancelStatementExecute(DBRProgressMonitor monitor, JDBCStatement statement) throws DBException {
		if (driverSupportsQueryCancel()) {
			super.cancelStatementExecute(monitor, statement);
		} else {
			// 若数据库不支持单词查询取消，则取消会话
			try {
				Connection connection = statement.getConnection().getOriginal();
				BeanUtils.invokeObjectMethod(connection, "cancel");
			} catch (Throwable e) {
				throw new DBException("无法取消会话查询", e, this);
			}
		}
	}

	private boolean driverSupportsQueryCancel() {
		return true;
	}

	@NotNull
	@Override
	public DataSource getDataSource() {
		return (DataSource) this.getContainer().getDataSource();
	}

	@NotNull
	@Override
	public DBPDataKind resolveDataKind(@NotNull String typeName, int valueType) {
		if ((typeName.equals(Constants.TYPE_NAME_XML) || typeName.equals(Constants.TYPE_FQ_XML))) {
			return DBPDataKind.CONTENT;
		}
		DBPDataKind dataKind = DataType.getDataKind(typeName);
		if (dataKind != null) {
			return dataKind;
		}
		return super.resolveDataKind(typeName, valueType);
	}

	@Override
	public Collection<? extends DBSDataType> getLocalDataTypes() {
		return dataTypeCache.getCachedObjects();
	}

	@Override
	public DBSDataType getLocalDataType(String typeName) {
		return dataTypeCache.getCachedObject(typeName);
	}

	@Nullable
	@Override
	public DBSDataType resolveDataType(@NotNull DBRProgressMonitor monitor, @NotNull String typeFullName)
			throws DBException {
		int divPos = typeFullName.indexOf(SQLConstants.STRUCT_SEPARATOR);
		if (divPos == -1) {
			// 获取简单类型名称
			return getLocalDataType(typeFullName);
		} else {
			return null;
		}
	}

	@Nullable
	@Override
	public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
		return super.createQueryTransformer(type);
	}

	private static final Pattern ERROR_POSITION_PATTERN_1 = Pattern.compile(".+\\s+line ([0-9]+), column ([0-9]+)");
	private static final Pattern ERROR_POSITION_PATTERN_2 = Pattern.compile(".+\\s+at line ([0-9]+)");
	private static final Pattern ERROR_POSITION_PATTERN_3 = Pattern.compile(".+\\s+at position\\: ([0-9]+)");

	@Nullable
	@Override
	public ErrorPosition[] getErrorPosition(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context,
			@NotNull String query, @NotNull Throwable error) {
		while (error instanceof DBException) {
			if (error.getCause() == null) {
				break;
			}
			error = error.getCause();
		}
		String message = error.getMessage();
		if (!CommonUtils.isEmpty(message)) {
			List<ErrorPosition> positions = new ArrayList<>();
			Matcher matcher = ERROR_POSITION_PATTERN_1.matcher(message);
			while (matcher.find()) {
				DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
				pos.info = matcher.group(1);
				pos.line = Integer.parseInt(matcher.group(1)) - 1;
				pos.position = Integer.parseInt(matcher.group(2)) - 1;
				positions.add(pos);
			}
			if (positions.isEmpty()) {
				matcher = ERROR_POSITION_PATTERN_2.matcher(message);
				while (matcher.find()) {
					DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
					pos.info = matcher.group(1);
					pos.line = Integer.parseInt(matcher.group(1)) - 1;
					positions.add(pos);
				}
			}
			if (positions.isEmpty()) {
				matcher = ERROR_POSITION_PATTERN_3.matcher(message);
				while (matcher.find()) {
					DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
					pos.info = matcher.group(1);
					pos.position = Integer.parseInt(matcher.group(1)) - 1;
					positions.add(pos);
				}
			}

			if (!positions.isEmpty()) {
				return positions.toArray(new ErrorPosition[positions.size()]);
			}
		}
		if (error.getCause() != null) {
			// 可能是数据库异常
			try {
				Object errorPosition = BeanUtils.readObjectProperty(error.getCause(), "errorPosition");
				if (errorPosition instanceof Number) {
					DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
					pos.position = ((Number) errorPosition).intValue();
					return new ErrorPosition[] { pos };
				}
			} catch (Exception e) {
				// 不是数据库异常
				log.debug("无法读取对象属性：" + e.getMessage());
			}
		}
		if (error instanceof SQLException
				&& SQLState.SQL_42000.getCode().equals(((SQLException) error).getSQLState())) {
			try (JDBCSession session = (JDBCSession) context.openSession(monitor, DBCExecutionPurpose.UTIL,
					"Extract last error position")) {
				try (CallableStatement stat = session
						.prepareCall("DECLARE\n" + "  L_CURSOR INTEGER DEFAULT DBMS_SQL.OPEN_CURSOR; \n" + "BEGIN \n"
								+ "  BEGIN \n" + "  DBMS_SQL.PARSE(  L_CURSOR, ?, DBMS_SQL.NATIVE ); \n"
								+ "    EXCEPTION \n" + "      WHEN OTHERS THEN ? := DBMS_SQL.LAST_ERROR_POSITION; \n"
								+ "    END; \n" + "    DBMS_SQL.CLOSE_CURSOR( L_CURSOR );\n" + "END;")) {
					stat.setString(1, query);
					stat.registerOutParameter(2, Types.INTEGER);
					stat.execute();
					int errorPos = stat.getInt(2);
					if (errorPos <= 0) {
						return null;
					}

					DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
					pos.position = errorPos;
					return new ErrorPosition[] { pos };

				} catch (SQLException e) {
					log.debug("无法提取解析错误信息：" + e.getMessage());
				}
			}
		}
		return null;
	}

	private class OutputReader extends AsyncServerOutputReader {
		@Override
		public boolean isServerOutputEnabled() {
			return getContainer().getPreferenceStore().getBoolean(Constants.PREF_DBMS_OUTPUT);
		}

		@Override
		public boolean isAsyncOutputReadSupported() {
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void readServerOutput(@NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionContext context,
				@Nullable SQLQueryResult queryResult, @Nullable DBCStatement statement, @NotNull PrintWriter output)
				throws DBCException {
			try {
				if (statement == null) {
					if (queryResult != null) {
			            dumpWarnings(output, queryResult.getWarnings());
			        }
	            } else {
	            	Object originStatement = getOriginalStatement(statement);
					Class<?> oemStatementClass =Class.forName(String.format("com.%s.cloudjdbc.Statement", OemConfig.OEM_NAME_EN_LOWER));
					Method method = oemStatementClass.getMethod("getSqlsEffectCountVector");
					Vector<Vector<Object>> messageVector = (Vector<Vector<Object>>) method.invoke(originStatement);
					messageVector.forEach((messageColumnVector) -> {
						Object object = messageColumnVector.get(1);
						if (object instanceof String) {
							String type = (String) object;
							if (M.equalsIgnoreCase(type)) {
								output.append((String) messageColumnVector.get(0));
							}
						}
					});
					
					Throwable[] statementWarnings = statement.getStatementWarnings();
	                if (statementWarnings != null && statementWarnings.length > 0) {
	                	output.println("---警告---");
	                    dumpWarnings(output, Arrays.asList(statementWarnings));
	                }
	            }
			} catch(ClassNotFoundException ignore) {
				// 忽略驱动未注入期间调用此方式产生的类未找到异常
			} catch (Exception e) {
				throw new DBCException("获取原始Statement失败", e);
			}
		}
		
		/**
		 * 由于JDBCStatementImpl获取原始Statement方法为protect权限<br>
		 * 因此使用反射调用方法获取原始Statement
		 * 
		 * @param dbcStatement DBeaver传入Statement封装对象
		 * @return 原始Statement对象
		 * @throws Exception 当方法对象获取失败或者方法调用失败时抛出异常
		 */
		private Object getOriginalStatement(DBCStatement dbcStatement) throws Exception {
			Class<?> clazz = JDBCStatementImpl.class;
			Method method = clazz.getDeclaredMethod("getOriginal");
			boolean accessible = method.isAccessible();
			method.setAccessible(true);
			Object returnObject = method.invoke((JDBCStatementImpl) dbcStatement);
			method.setAccessible(accessible);
			return   returnObject;
		}
	}

	@NotNull
	@Override
	public DBCPlan planQueryExecution(@NotNull DBCSession session, @NotNull String query) throws DBException {
		PlanAnalyser plan = new PlanAnalyser(this, (JDBCSession) session, query);
		plan.explain();
		return plan;
	}

	@NotNull
	@Override
	public DBCPlanStyle getPlanStyle() {
		return DBCPlanStyle.PLAN;
	}

	/**
	 * 数据库缓存
	 */
	public static class DatabaseCache extends JDBCStructLookupCache<DataSource, Database, Schema> {

		public DatabaseCache() {
			super("DB_NAME");
		}

		/**
		 * 缓存库信息
		 */
		@Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, DataSource owner, Database object,
				String objectName) throws SQLException {
			return session.prepareStatement("SHOW DB_INFO");
		}

		@Override
		protected Database fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new Database(owner, resultSet);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, DataSource owner, Database forObject)
				throws SQLException {
			return null;
		}

		@Override
		protected Schema fetchChild(JDBCSession session, DataSource owner, Database parent, JDBCResultSet dbResult)
				throws SQLException, DBException {
			return null;
		}
	}

	/**
	 * 模式缓存
	 */
	public static class SchemaCache extends JDBCStructLookupCache<Database, Schema, Schema> {
		SchemaCache() {
			super("SCHEMA_NAME");
			setListOrderComparator(DBUtils.<Schema>nameComparator());
		}

		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull Database owner,
				Schema schema, String name) throws SQLException {
			StringBuilder schemasQuery = new StringBuilder();
			String dbName = session.getCatalog();
			// 根据owner的用户角色选取不同的语句来查询schema
			schemasQuery.append("SELECT S.DB_ID,S.SCHEMA_ID,S.SCHEMA_NAME,U.USER_NAME,S.COMMENTS FROM ");
			schemasQuery.append("ALL");
			schemasQuery.append("_SCHEMAS S");
			schemasQuery.append(",");
			schemasQuery.append("ALL");
			schemasQuery.append("_USERS U");
			schemasQuery.append(" WHERE S.USER_ID=U.USER_ID AND S.DB_ID=");
			schemasQuery.append(owner.getId());
			if (schema != null) {
				schemasQuery.append(" AND S.SCHEMA_NAME =");
				schemasQuery.append(SQLUtils.quoteString(schema, schema.getName()));
			} else if (name != null) {
				schemasQuery.append(" AND S.SCHEMA_NAME =");
				schemasQuery.append(SQLUtils.quoteString(owner, name));
			}
			schemasQuery.append(" ORDER BY S.SCHEMA_ID ASC");
			log.debug("schema message ：" + schemasQuery.toString()); 

			JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());

			return dbStat;
		}

		@Override
		protected Schema fetchObject(@NotNull JDBCSession session, @NotNull Database owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new Schema(owner.getDataSource(), resultSet);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, Database owner, Schema forObject)
				throws SQLException {
			// TODO 准备模式缓存子对象声明
			return null;
		}

		@Override
		protected Schema fetchChild(JDBCSession session, Database owner, Schema parent, JDBCResultSet dbResult)
				throws SQLException, DBException {
			// TODO 获取模式缓存子对象
			return null;
		}
	}

	/**
	 * 数据类型缓存，不做查询操作，在 initialize 函数中进行初始化
	 */
	static class DataTypeCache extends JDBCObjectCache<DataSource, DataType> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DataSource owner)
				throws SQLException {
			// TODO 准备数据类型缓存对象声明
			return session.prepareStatement("");
		}

		@Override
		protected DataType fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			// TODO 获取数据类型缓存对象
			return null;
		}
	}

	/**
	 * 表空间缓存
	 */
	static class TablespaceCache extends JDBCObjectCache<DataSource, Tablespace> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DataSource owner)
				throws SQLException {
			// xfc 修改了获取表空间信息的sql语句
			return session.prepareStatement("SELECT * FROM " + owner.roleFlag + "_TABLESPACES");
		}

		@Override
		protected Tablespace fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new Tablespace(owner, resultSet);
		}
	}

	/**
	 * 用户缓存
	 */
	public static class UserCache extends JDBCStructLookupCache<DataSource, User, User> {
		public UserCache() {
			super("USER_NAME");
			setListOrderComparator(DBUtils.<User>nameComparator());
		}

		@Override
		protected User fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new User(owner, resultSet, session.getProgressMonitor(), true);
		}

		@Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, DataSource owner, User user, String objectName)
				throws SQLException {
			StringBuilder sql = new StringBuilder("SELECT * FROM ");
			String dbName = session.getCatalog();
			try {
				sql.append(owner.getRoleFlag());
				sql.append("_USERS");
				sql.append(" WHERE IS_ROLE=FALSE AND DB_ID=");
				sql.append(owner.databaseCache.getObject(session.getProgressMonitor(), owner, dbName).getId());
			} catch (DBException e) {
				throw new SQLException("Get database object error: ", e);
			}
			if (user != null) {
				sql.append(" AND USER_ID =");
				sql.append(user.getUserId());
			}
			return session.prepareStatement(sql.toString());
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, DataSource owner, User forObject)
				throws SQLException {
			// TODO 准备用户缓存子对象声明
			return null;
		}

		@Override
		protected User fetchChild(JDBCSession session, DataSource owner, User parent, JDBCResultSet dbResult)
				throws SQLException, DBException {
			// TODO 获取用户缓存子对象
			return null;
		}
	}

	/**
	 * 角色缓存
	 */
	public class RoleCache extends JDBCStructLookupCache<DataSource, Role, Role> {
		public RoleCache() {
			super("ROLE_NAME");
			setListOrderComparator(DBUtils.<Role>nameComparator());
		}

		@Override
		protected Role fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			if (resultSet != null) {
				return new Role(owner, session.getProgressMonitor(), resultSet);
			} else {
				return null;
			}
		}

		@Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, DataSource owner, Role object,
				String objectName) throws SQLException {
			StringBuilder sql = new StringBuilder();
			String dbName = session.getCatalog();
			try {
				sql.append("SELECT * FROM ");
				sql.append(owner.getRoleFlag());
				sql.append("_USERS WHERE IS_ROLE=true");
				sql.append(" AND DB_ID=");
				sql.append(owner.databaseCache.getObject(session.getProgressMonitor(), owner, dbName).getId());
			} catch (DBException e) {
				throw new SQLException("Error in DataSource.RoleCache.prepareObjectsStatement()", e);
			}
			if (object != null) {
				sql.append(" AND USER_ID =");
				sql.append(object.getId());
			}
			return session.prepareStatement(sql.toString());
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, DataSource owner, Role forObject)
				throws SQLException {
			return null;
		}

		@Override
		protected Role fetchChild(JDBCSession session, DataSource owner, Role parent, JDBCResultSet dbResult)
				throws SQLException, DBException {
			return null;
		}
	}
	
	/**
	 * 作业缓存
	 */
	public static class SchedulerJobCache extends JDBCStructLookupCache<DataSource, SchedulerJob, SchedulerJob> {
		public SchedulerJobCache() {
			super("SCHEDULER_JOB_NAME");
			setListOrderComparator(DBUtils.<SchedulerJob>nameComparator());
		}

		@Override
		public JDBCStatement prepareLookupStatement(JDBCSession session, DataSource owner, SchedulerJob object,
				String objectName) throws SQLException {
			// xfc 修改了获取所有job信息的sql语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			String dbName = session.getCatalog();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_JOBS WHERE DB_ID=");
			try {
				sql.append(owner.databaseCache.getObject(session.getProgressMonitor(), owner, objectName).getId());
			} catch (DBException e) {
				throw new SQLException("Error in DataSource.SchedulerJobCache.prepareObjectsStatement()", e);
			}

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select jobs sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected SchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new SchedulerJob(session.getProgressMonitor(), (DataSource)session.getDataSource().getDataSource(), dbResult);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, DataSource owner, SchedulerJob forObject)
				throws SQLException {
			return null;
		}

		@Override
		protected SchedulerJob fetchChild(JDBCSession session, DataSource owner, SchedulerJob parent,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return null;
		}
	}

	/**
	 * 全局同义词缓存
	 */
	static class SynonymCache extends JDBCStructLookupCache<DataSource, PublicSynonym, PublicSynonym> {
		public SynonymCache() {
			super("SYNO_NAME");
			setListOrderComparator(DBUtils.<PublicSynonym>nameComparator());
		}

		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull DataSource owner, PublicSynonym object, String objectName)
				throws SQLException {
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("select s3.schema_name TARG_SC, s1.*  from ");
			sql.append(roleFlag);
			sql.append("_synonyms s1 left join ");
			sql.append(roleFlag);
			sql.append("_schemas s3  ON s3.schema_id=s1.targ_sche_id AND s3.db_id=current_db_id  ");
			sql.append( " where s1.is_public = true ");
			log.debug("" + OemConfig.OEM_NAME_EN + " public synonyms metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected PublicSynonym fetchObject(@NotNull JDBCSession session, @NotNull DataSource owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new PublicSynonym(owner, resultSet);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(JDBCSession session, DataSource owner, PublicSynonym forObject)
				throws SQLException {
			return null;
		}

		@Override
		protected PublicSynonym fetchChild(JDBCSession session, DataSource owner, PublicSynonym parent,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return null;
		}
	}

	public Collection<Charset> getCharsets() {
		return charsets;
	}

	public Charset getCharset(String name) {
		for (Charset charset : charsets) {
			if (charset.getName().equals(name)) {
				return charset;
			}
		}
		return null;
	}

	public TablespaceCache getTablespaceCache() {
		return tablespaceCache;
	}

	public String getActiveSchemaName() {
		return activeSchemaName;
	}

	/**
	 * 从作业缓存中获取全部的作业信息
	 * 
	 * @param monitor 监控
	 * @return list 作业列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<SchedulerJob> getSchedulerJobs(DBRProgressMonitor monitor) throws DBException {
		Collection<SchedulerJob> list = schedulerJobCache.getAllObjects(monitor, this);
		return list;
	}

	/**
	 * 从同义词缓存中获取全部的同义词信息
	 * 
	 * @param monitor 监控
	 * @return list 同义词列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<PublicSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
		Collection<PublicSynonym> list = synonymCache.getAllObjects(monitor, this);
		return list;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}

	@Override
	public Connection getConnection() throws SQLException {
		DBPConnectionConfiguration config = this.getContainer().getConnectionConfiguration();
		String url = config.getUrl();
		Properties prop = new Properties();
		prop.put("user", config.getUserName());
		prop.put("password", config.getUserPassword());
		return driver.connect(url, prop);
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		DBPConnectionConfiguration config = this.getContainer().getConnectionConfiguration();
		String url = config.getUrl();
		Properties prop = new Properties();
		prop.put("user", username);
		prop.put("password", password);
		return driver.connect(url, prop);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		throw new UnsupportedOperationException("数据源暂未支持此方法");
	}
}
