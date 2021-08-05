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
import org.jkiss.dbeaver.ext.xugu.model.source.StatefulObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 * 作业信息类，包含作业相关的基本信息，以及作业参数缓存
 */
public class SchedulerJob extends BaseSchemaObject implements StatefulObject, DBPScriptObjectExt {
	private int jobId;
	private int dbId;
	private int userId;
	private String jobName;
	private int grpId;
	private int jobNo;
	private String jobType;
	private int paramNum;
	private String paramDef;
	private String actionDef;
	private Date beginTime;
	private Date endTime;
	private String repetInterval;
	private String trigEvents;
	private Date lastTime;
	private String state;
	private boolean enable;
	private boolean autoDrop;
	private boolean isSys;
	private String comments;
	private Collection<ProcedureParameter> procParams;

	private final ArgumentsCache argumentsCache = new ArgumentsCache();

	enum JobState {
		/**
		 * 任务状态枚举
		 */
		DISABLED, RETRYSCHEDULED, SCHEDULED, RUNNING, COMPLETED, BROKEN, FAILED, REMOTE, SUCCEEDED, CHAIN_STALLED;
	}

	protected SchedulerJob(DBRProgressMonitor monitor, JDBCSession session, Schema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "JOB_NAME"), true);

		dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
		userId = JDBCUtils.safeGetInt(dbResult, "USER_ID");
		jobName = JDBCUtils.safeGetString(dbResult, "JOB_NAME");
		grpId = JDBCUtils.safeGetInt(dbResult, "JOB_GRP_ID");
		jobNo = JDBCUtils.safeGetInt(dbResult, "JOB_NO");
		jobType = JDBCUtils.safeGetString(dbResult, "JOB_TYPE");
		paramNum = JDBCUtils.safeGetInt(dbResult, "JOB_PARAM_NUM");
		paramDef = JDBCUtils.safeGetString(dbResult, "JOB_PARAM");
		actionDef = JDBCUtils.safeGetString(dbResult, "JOB_ACTION");
		jobType = JDBCUtils.safeGetString(dbResult, "JOB_TYPE");
		beginTime = JDBCUtils.safeGetDate(dbResult, "BEGIN_T");
		endTime = JDBCUtils.safeGetDate(dbResult, "END_T");
		repetInterval = JDBCUtils.safeGetString(dbResult, "REPET_INTERVAL");
		trigEvents = JDBCUtils.safeGetString(dbResult, "TRIG_EVENTS");
		lastTime = JDBCUtils.safeGetDate(dbResult, "LAST_RUN_T");
		state = JDBCUtils.safeGetString(dbResult, "STATE");
		enable = JDBCUtils.safeGetBoolean(dbResult, "ENABLE");
		autoDrop = JDBCUtils.safeGetBoolean(dbResult, "AUTO_DROP");
		isSys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
		comments = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		// 加载参数信息和Action信息
		String targetPro = actionDef;
		// 定义中包含有存储过程
		final String separator = ".";
		if (targetPro.indexOf(separator) != -1) {
			targetPro = targetPro.substring(targetPro.indexOf(".") + 1, targetPro.length());
			try {
				// 目标尚未被缓存
				if (schema.proceduresCache.getCachedObject(targetPro) == null) {
					try {
						StringBuilder sql = new StringBuilder();
						sql.append("SELECT * FROM ");
						sql.append(schema.getRoleFlag());
						sql.append("_PROCEDURES WHERE SCHEMA_ID=");
						sql.append(schema.getId());
						sql.append(" AND PROC_NAME = '");
						sql.append(targetPro);
						sql.append("'");
						JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
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
							this.actionDef = pro.getObjectDefinitionText(monitor, null);
						}
						dbStat.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				} else {
					if (this.paramNum != 0) {
						this.procParams = schema.proceduresCache.getCachedObject(targetPro).getParameters(monitor);
					}
					this.actionDef = schema.proceduresCache.getCachedObject(targetPro).getObjectDefinitionText(monitor,
							null);
				}
			} catch (DBException e) {
				e.printStackTrace();
			}
		}
	}

	public int getJobId() {
		return jobId;
	}

	public int getDbId() {
		return dbId;
	}

	public String getAction() {
		return actionDef;
	}

	public int getUserId() {
		return userId;
	}

	public String getJobName() {
		return jobName;
	}

	public int getGrpId() {
		return grpId;
	}

	public int getJobNo() {
		return jobNo;
	}

	public String getJobType() {
		return jobType;
	}

	public int getParamNum() {
		return paramNum;
	}

	public String getParamDef() {
		return paramDef;
	}

	public Date getBeginTime() {
		return beginTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public String getRepetInterval() {
		return repetInterval;
	}

	public String getTrigEvents() {
		return trigEvents;
	}

	public Date getLastTime() {
		return lastTime;
	}

	public String getState() {
		return state;
	}

	public boolean isEnable() {
		return enable;
	}

	public boolean isAutoDrop() {
		return autoDrop;
	}

	public boolean isSys() {
		return isSys;
	}

	public String getComments() {
		return comments;
	}

	@Association
	public Collection<ProcedureParameter> getArguments(DBRProgressMonitor monitor) throws DBException {
		return this.procParams;
	}

	static class ArgumentsCache extends JDBCObjectCache<SchedulerJob, SchedulerJobArgument> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SchedulerJob job)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT JOB_PARAM_NUM, JOB_ACTION FROM "
					+ job.getSchema().getRoleFlag() + "_JOBS " + "WHERE JOB_ID=? ");
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
	public void refreshObjectState(DBRProgressMonitor monitor) {
		if (monitor != null) {
			monitor.beginTask("Load action for '" + this.getName() + "'...", 1);
			try (final JDBCSession session = DBUtils.openMetaSession(monitor, this,
					"Load action for " + ObjectType.JOB + " '" + this.getName() + "'")) {
				try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT STATE FROM "
						+ this.getDataSource().getRoleFlag() + "_JOBS " + "WHERE DB_ID=? AND JOB_NAME=? ")) {
					dbStat.setString(1, getDbId() + "");
					dbStat.setString(2, getName());
					dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						StringBuilder jobState = null;
						int lineCount = 0;
						while (dbResult.next()) {
							if (monitor.isCanceled()) {
								break;
							}
							final String line = dbResult.getString(1);
							if (jobState == null) {
								jobState = new StringBuilder(15);
							}
							jobState.append(line);
							lineCount++;
							monitor.subTask("Line " + lineCount);
						}
						if (jobState != null) {
							state = jobState.toString();
						}
					}
				}
			} catch (SQLException e) {
				monitor.subTask("Error refreshing job state " + e.getMessage());
			} finally {
				monitor.done();
			}
		}
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return "ACTION STRING";
	}

	@Override
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		// TODO 获取拓展定义文本，生成包含实体任务定义的 DDL，而不仅仅是动作块
		return null;
	}

	@Override
	public DBSObjectState getObjectState() {
		DBSObjectState objectState = null;
		try {
			final String stateIdle = "IDLE";
			if (stateIdle.equals(state)) {
				objectState = DBSObjectState.ACTIVE;
			} else {
				objectState = DBSObjectState.NORMAL;
			}
		} catch (IllegalArgumentException e) {
			objectState = DBSObjectState.UNKNOWN;
		}
		return objectState;
	}
}
