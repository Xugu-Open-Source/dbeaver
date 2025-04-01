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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.SchedulerJob;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import cn.hutool.core.lang.UUID;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 定时作业管理器，进行定时作业的创建，修改和删除
 */
public class SchedulerJobManager extends SQLObjectEditor<SchedulerJob, Schema> implements DBEObjectRenamer<SchedulerJob> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, SchedulerJob> getObjectsCache(SchedulerJob object) {
		return object.getDataSource().schedulerJobCache;
	}

	@Override
	protected SchedulerJob createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		DataSource datasource = (DataSource) container;
		String namePrefix = "newjob_";
		String newJobName = null;
		for (int i = 1; i <= Integer.MAX_VALUE; ++i) {
			String tempName = namePrefix + i;
			if (datasource.schedulerJobCache.getCachedObject(tempName) == null) {
				newJobName = tempName;
				break;
			}
		}
		if (newJobName == null) {
			newJobName = namePrefix + UUID.randomUUID().toString(true);
		}
		return new SchedulerJob(datasource, newJobName, false);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<SchedulerJob, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		addCreateAction(monitor, executionContext, actions, command, options);
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<SchedulerJob, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		addDeleteAction(monitor, executionContext, actions, command, options);
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<SchedulerJob, Schema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		if (command.hasProperty("enable") && command.getProperties().size() == 1) {
			boolean enable = (Boolean) command.getProperty("enable");
			if (enable) {
				addEnableAction(monitor, executionContext, actions, command, options);
			} else {
				addDisableAction(monitor, executionContext, actions, command, options);
			}
		} else if (command.hasProperty("runOnceNow") && command.getProperties().size() == 1) {
			addRunAction(monitor, executionContext, actions, command, options);
		} else if(command.hasProperty("enable") && command.hasProperty("runOnceNow") && command.getProperties().size() == 2) {
			boolean enable = (Boolean) command.getProperty("enable");
			if (enable) {
				addEnableAction(monitor, executionContext, actions, command, options);
			} else {
				addDisableAction(monitor, executionContext, actions, command, options);
			}
			addRunAction(monitor, executionContext, actions, command, options);
		} else {
			addDeleteAction(monitor, executionContext, actions, command, options);
			addCreateAction(monitor, executionContext, actions, command, options);
		}
	}
	
	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<SchedulerJob, Schema>.ObjectRenameCommand command,
			Map<String, Object> options) {
		command.getObject().setName(command.getOldName());
		addDeleteAction(monitor, executionContext, actions, command, options);
		command.getObject().setName(command.getNewName());
		addCreateAction(monitor, executionContext, actions, command, options);
	}

	private void addCreateAction(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<SchedulerJob> command,
			Map<String, Object> options) {
		SchedulerJob job = command.getObject();
		StringBuilder builder = new StringBuilder("dbms_scheduler.create_job(");
		//必选参数
		String jobName = Optional.ofNullable(job.getName())
				.orElseThrow(()-> new IllegalArgumentException("作业名称不能为空"));
		builder.append("'").append(jobName).append("'");
		String jobType = Optional.ofNullable(job.getJobType())
				.orElseThrow(()-> new IllegalArgumentException("作业类型不能为空"));
		builder.append(",'").append(jobType).append("'");
		String jobAction = Optional.ofNullable(job.getActionDef())
				.orElseThrow(()-> new IllegalArgumentException("作业动作不能为空"));
		builder.append(",'").append(jobAction).append("'");
		//可选参数
		builder.append(",").append(Optional.ofNullable(job.getParamNum()).orElse(0));
		String beginTime = job.getBeginTime();
		if (beginTime == null) {
			builder.append(",null");
		} else {
			builder.append(",'").append(beginTime).append("'");
		}
		
		String repetInterval = job.getRepetInterval();
		if (repetInterval == null) {
			builder.append(",null");
		} else {
			builder.append(",'").append(repetInterval).append("'");
		}

		String endTime = job.getEndTime();
		if (endTime == null) {
			builder.append(",null");
		} else {
			builder.append(",'").append(endTime).append("'");
		}

		builder.append(",'default_class'");
		builder.append(",").append(job.isEnable());
		builder.append(",").append(job.isAutoDrop());
		
		String comments = job.getComments();
		if (comments == null) {
			builder.append(",null)");
		} else {
			builder.append(",'").append(comments).append("')");
		}

		String sql = builder.toString();
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create scheduler job sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create scheduler job", sql));
		
		String paramDef = job.getParamDef();
		if (paramDef != null) {
			String[] params = paramDef.split(",");
			for (int i = 0; i < params.length; ++i) {
				sql = String.format("DBMS_SCHEDULER.SET_JOB_ARGUMENT_VALUE('%s',%d,%s)", jobName, i+1, params[i]);
				actions.add(new SQLDatabasePersistAction("Set scheduler job arguments", sql));
			}
		}
		if (job.isRunOnceNow()) {
			addRunAction(monitor, executionContext, actions, command, options);
		}
	}
	
	private void addDeleteAction(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<SchedulerJob> command,
			Map<String, Object> options) {
		String sql = "dbms_scheduler.drop_job('" + command.getObject().getName() + "',false)";
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop scheduler job sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop scheduler job", sql));
	}
	
	private void addEnableAction(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<SchedulerJob> command,
			Map<String, Object> options) {
		String sql = String.format("dbms_scheduler.enable('%s')", command.getObject().getName());
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct enable scheduler job sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Enable scheduler job", sql));
	}
	
	private void addDisableAction(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<SchedulerJob> command,
			Map<String, Object> options) {
		String sql = String.format("dbms_scheduler.disable('%s',false)", command.getObject().getName());
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct disable scheduler job sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Disable scheduler job", sql));
	}

	private void addRunAction(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<SchedulerJob> command,
			Map<String, Object> options) {
		String sql = String.format("dbms_scheduler.run_job('%s',true)", command.getObject().getName());
		actions.add(new SQLDatabasePersistAction("Run scheduler job once now", sql));
	}

	@Override
	public void renameObject(DBECommandContext commandContext, SchedulerJob object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}
}
