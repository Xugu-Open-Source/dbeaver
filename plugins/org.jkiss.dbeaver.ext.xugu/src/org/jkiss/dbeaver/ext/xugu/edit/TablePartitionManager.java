/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.xugu.model.BaseTablePhysical;
import org.jkiss.dbeaver.ext.xugu.model.TablePartition;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * 表分区管理器，进行表分区的创建和删除，修改仅支持设定是否在线，包含一个内部界面类，用于进行属性设定
 */
public class TablePartitionManager extends SQLObjectEditor<TablePartition, BaseTablePhysical> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	protected TablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
        return new TablePartition((BaseTablePhysical)container,false,"");
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TablePartition, BaseTablePhysical>.ObjectCreateCommand command, Map<String, Object> options)
			throws DBException {
		// 新建表时在tablemanager中进行添加处理
		// 修改已存在表的分区时新增修改语句
		if (command.getObject().getParentObject().isPersisted() == true) {
			StringBuilder sql = new StringBuilder();

			sql.append("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			sql.append(" ADD PARTITION ");
			sql.append(command.getObject().getName());
			switch (command.getObject().getPartiType()) {
			case "LIST":
				sql.append(" VALUES('");
				sql.append(command.getObject().getPartiValue());
				sql.append("')");
				break;
			case "RANGE":
				sql.append(" VALUES LESS THAN(");
				sql.append(command.getObject().getPartiValue());
				sql.append(")");
				break;
			case "AUTOMATIC":
				sql.append(" VALUES LESS THAN(");
				sql.append(command.getObject().getPartiValue());
				sql.append(")");
				break;
			default:
				break;
			}

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add table partition sql: " + sql.toString());
			actions.add(new SQLDatabasePersistAction("Modify table, Add Partition", sql.toString()));
		} else {
			command.getObject().getParentObject().partitionCache.cacheObject(command.getObject());
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TablePartition, BaseTablePhysical>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		// 当表存在时才可进行删除action
		if (command.getObject().getParentObject().isPersisted() == true) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			sql.append(" DROP PARTITION ");
			sql.append(command.getObject().getName());

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop table partition sql: " + sql.toString());
			actions.add(new SQLDatabasePersistAction("Drop Partition", sql.toString()));
		}
		// 若是新增表情况时则直接将改对象从缓存中剔除
		else {
			command.getObject().getParentObject().partitionCache.removeObject(command.getObject(), false);
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList,
			SQLObjectEditor<TablePartition, BaseTablePhysical>.ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		// 当表存在时才可进行修改 action
		final String onlineKey = "online";
		if (command.getObject().getParentObject().isPersisted() == true && command.getProperty(onlineKey) != null) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			sql.append(" SET PARTITION ");
			sql.append("\"" + command.getObject().getName() + "\"");
			sql.append((boolean) command.getProperty("online") ? " ONLINE" : " OFFLINE");

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter table partition sql: " + sql.toString());
			actionList.add(new SQLDatabasePersistAction("Alter Partition", sql.toString()));
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, TablePartition> getObjectsCache(TablePartition object) {
		return object.getParentObject().partitionCache;
	}
}
