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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.SchedulerJobCache;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.UserRoleFlag;
import org.jkiss.dbeaver.ext.xugu.model.source.StatefulObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import com.xugu.parser.DatabaseParsing;
import com.xugu.parser.Parsing;
import com.xugu.parser.Parsing.TableType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * 作业信息类，包含作业相关的基本信息，以及作业参数缓存
 */
public class SchedulerJob extends BaseGlobalObject implements DBPScriptObject, DBPNamedObject, DBPRefreshableObject {
	private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";
	private static final ThreadLocal<SimpleDateFormat> DATE_FORMATTER = ThreadLocal.withInitial(() -> new SimpleDateFormat(DATE_FORMAT_PATTERN));

	private String name;
	private int jobId;
	private int dbId;
	private int userId;
	private int grpId;
	private int jobNo;
	private String jobType;
	private int paramNum;
	private String paramDef;
	private String actionDef;
	private Timestamp beginTime;
	private Timestamp endTime;
	private String repetInterval;
	private String trigEvents;
	private Timestamp lastTime;
	private String state;
	private boolean enable = false;
	private boolean autoDrop = true;
	private boolean isSys = false;
	private boolean runOnceNow = false;
	private String comments;
	private Collection<ProcedureParameter> procParams;
	private Database parent;

	private final ArgumentsCache argumentsCache = new ArgumentsCache();

	enum JobState {
		/**
		 * 任务状态枚举
		 */
		DISABLED, RETRYSCHEDULED, SCHEDULED, RUNNING, COMPLETED, BROKEN, FAILED, REMOTE, SUCCEEDED, CHAIN_STALLED;
	}
	
	public SchedulerJob(DataSource datasource, String name, boolean persisted) {
		super(datasource, persisted);
		this.name = name;
		this.parent = datasource.getDatabase();
	}

	protected SchedulerJob(DBRProgressMonitor monitor, DataSource datasource, ResultSet dbResult) {
		super(datasource, true);
		this.parent = datasource.getDatabase();

		jobId = JDBCUtils.safeGetInt(dbResult, "JOB_ID");
		dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
		userId = JDBCUtils.safeGetInt(dbResult, "USER_ID");
		name = JDBCUtils.safeGetString(dbResult, "JOB_NAME");
		grpId = JDBCUtils.safeGetInt(dbResult, "JOB_GRP_ID");
		jobNo = JDBCUtils.safeGetInt(dbResult, "JOB_NO");
		jobType = JDBCUtils.safeGetString(dbResult, "JOB_TYPE");
		paramNum = JDBCUtils.safeGetInt(dbResult, "JOB_PARAM_NUM");
		paramDef = null; // 已设置的存储过程参数无法获取其字符表示，服务器中将其转换并存储为二进制数据
		actionDef = JDBCUtils.safeGetString(dbResult, "JOB_ACTION");
		jobType = JDBCUtils.safeGetString(dbResult, "JOB_TYPE");
		beginTime = JDBCUtils.safeGetTimestamp(dbResult, "BEGIN_T");
		endTime = JDBCUtils.safeGetTimestamp(dbResult, "END_T");
		repetInterval = JDBCUtils.safeGetString(dbResult, "REPET_INTERVAL");
		trigEvents = JDBCUtils.safeGetString(dbResult, "TRIG_EVENTS");
		lastTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_RUN_T");
		state = JDBCUtils.safeGetString(dbResult, "STATE");
		enable = JDBCUtils.safeGetBoolean(dbResult, "ENABLE");
		autoDrop = JDBCUtils.safeGetBoolean(dbResult, "AUTO_DROP");
		isSys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
		comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		// 加载参数信息和Action信息
		if (jobType.equalsIgnoreCase("stored_procedure")) {
			try {
				// 目标尚未被缓存
				DataSource ds = (DataSource) datasource;
				String userName = ds.getContainer().getConnectionConfiguration().getUserName();
				String schemaName = null;
				String procedureName;
				StringBuilder builder = new StringBuilder();
				boolean isQuoted = false;
				// 从存储过程全名解析模式名与存储过程名
				for (int i = 0; i < actionDef.length(); i++) {
					char c = actionDef.charAt(i);
					if (c == '"') {
						isQuoted = !isQuoted;
						continue;
					}
					if (actionDef.charAt(i) == '.' && !isQuoted) {
						schemaName = builder.toString();
						builder = new StringBuilder();
					} else {
						builder.append(c);
					}
				}
				if (schemaName == null || schemaName.isEmpty()) {
					schemaName = userName;
				}
				procedureName = builder.toString();
				Schema schema = ds.schemaCache.getObject(monitor, ds.getDatabase(), schemaName);
				if (schema.proceduresCache.getObject(monitor, schema, procedureName) == null) {
					try {
						StringBuilder sql = new StringBuilder();
						sql.append("SELECT * FROM ");
						sql.append(schema.getRoleFlag());
						sql.append("_PROCEDURES WHERE SCHEMA_ID=");
						sql.append(schema.getId());
						sql.append(" AND PROC_NAME = '");
						sql.append(procedureName);
						sql.append("'");
						JDBCPreparedStatement dbStat = ds.getDefaultInstance()
								.getDefaultContext(true)
								.openSession(monitor, DBCExecutionPurpose.META, "Fetch scheduler job procedures meta data")
								.prepareStatement(sql.toString());
						ResultSet res = dbStat.executeQuery();
						if (res != null) {
							// 为了构造函数可以正常获取数据需要先遍历
							while (res.next()) {
								res.getInt(1);
								res.getInt(2);
								res.getInt(3);
								res.getInt(4);
								res.getString(5);
							}
							ProcedureStandalone pro = new ProcedureStandalone(monitor, schema, res);
							if (this.paramNum != 0) {
								this.procParams = pro.getParameters(monitor);
							}
						}
						dbStat.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} else {
					if (this.paramNum != 0) {
						this.procParams = schema.proceduresCache.getObject(monitor, schema, procedureName).getParameters(monitor);
					}
				}
			} catch (DBException e) {
				e.printStackTrace();
			}
		}
	}

	@Property(viewable = true, order = 10)
	public int getJobId() {
		return jobId;
	}

    @Property(viewable = true, order = 11)
	public int getDbId() {
		return dbId;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 12)
	public String getActionDef() {
		return actionDef;
	}

    @Property(viewable = true, order = 13)
	public int getUserId() {
		return userId;
	}

    @Property(viewable = true, order = 15)
	public int getGrpId() {
		return grpId;
	}

    @Property(viewable = true, order = 16)
	public int getJobNo() {
		return jobNo;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 17)
	public String getJobType() {
		return jobType;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 18)
	public int getParamNum() {
		return paramNum;
	}

    @Property(viewable = true, editable = true, order = 19)
	public String getParamDef() {
		return paramDef;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 20)
	public String getBeginTime() {
    	if (beginTime == null) {
    		return null;
    	}
		return DATE_FORMATTER.get().format(beginTime);
	}

    @Property(viewable = true, editable = true, updatable = true, order = 21)
	public String getEndTime() {
    	if (endTime == null) {
    		return null;
    	}
		return DATE_FORMATTER.get().format(endTime);
	}

    @Property(viewable = true, editable = true, updatable = true, order = 22)
	public String getRepetInterval() {
		return repetInterval;
	}

    @Property(viewable = true, order = 23)
	public String getTrigEvents() {
		return trigEvents;
	}

    @Property(viewable = true, order = 24)
	public String getLastTime() {
    	if (lastTime == null) {
    		return null;
    	}
		return DATE_FORMATTER.get().format(lastTime);
	}

    @Property(viewable = true, order = 25)
	public String getState() {
		return state;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 26)
	public boolean isEnable() {
		return enable;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 27)
	public boolean isAutoDrop() {
		return autoDrop;
	}

    @Property(viewable = true, order = 28)
	public boolean isSys() {
		return isSys;
	}

    @Property(viewable = true, editable = true, updatable = true, order = 29)
    public boolean isRunOnceNow() {
		return runOnceNow;
	}

	public void setRunOnceNow(boolean runOnceNow) {
		this.runOnceNow = runOnceNow;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 30)
	public String getComments() {
		return comments;
	}

	public Collection<ProcedureParameter> getArguments() {
		return procParams;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public void setParamNum(int paramNum) {
		this.paramNum = paramNum;
	}

	public void setParamDef(String paramDef) {
		this.paramDef = paramDef;
	}

	public void setActionDef(String actionDef) {
		this.actionDef = actionDef;
	}

	public void setBeginTime(String beginTime) {
		try {
			Date date = DATE_FORMATTER.get().parse(beginTime);
			this.beginTime = new Timestamp(date.getTime());
		} catch (ParseException e) {
			throw new IllegalStateException("开始时间格式错误，正确格式：" + DATE_FORMAT_PATTERN);
		}
	}

	public void setEndTime(String endTime) {
		try {
			Date date = DATE_FORMATTER.get().parse(endTime);
			this.endTime = new Timestamp(date.getTime());
		} catch (ParseException e) {
			throw new IllegalStateException("结束时间格式错误，正确格式：" + DATE_FORMAT_PATTERN);
		}
	}

	public void setRepetInterval(String repetInterval) {
		this.repetInterval = repetInterval;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void setAutoDrop(boolean autoDrop) {
		this.autoDrop = autoDrop;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	static class ArgumentsCache extends JDBCObjectCache<SchedulerJob, SchedulerJobArgument> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SchedulerJob job)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT JOB_PARAM_NUM, JOB_ACTION FROM "
					+ job.getDataSource().getRoleFlag() + "_JOBS " + "WHERE JOB_ID=? ");
			dbStat.setString(1, job.getJobId() + "");
			return dbStat;
		}

		@Override
		protected SchedulerJobArgument fetchObject(@NotNull JDBCSession session, @NotNull SchedulerJob job,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new SchedulerJobArgument(job, resultSet);
		}

	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		String objectFullName = DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL);
		monitor.beginTask("Load sources for schduler job '" + objectFullName + "'...", 1);
		try (Connection conn = DBUtils.openUtilSession(monitor, this, "Get " + this.name + "DDL")) {
			String roleFlag = getDataSource().getRoleFlag();
			TableType tableType;

			if (UserRoleFlag.SYS.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.SYS;
			} else if (UserRoleFlag.DBA.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.DBA;
			} else {
				tableType = TableType.ALL;
			}

			DatabaseParsing databaseParsing =new DatabaseParsing();
			return databaseParsing.loadJobDdl(conn, getDataSource().getDatabase().getId(), getName(), tableType);
		} catch (SQLException e) {
			throw new DBException("Close connection of DDL failed", e);
		}
	}

	@Override
    @Property(viewable = true, editable = true, updatable = true, order = 1)
	public String getName() {
		return name;
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		SchedulerJobCache cache = getDataSource().schedulerJobCache;
		cache.clearCache();
		return cache.refreshObject(monitor, getDataSource(), this);
	}
	
	public Database getParent() {
		return parent;
	}
}
