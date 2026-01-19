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
import org.jkiss.dbeaver.ext.xugu.model.TableSubPartition;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

/**
 * 二级分区管理器，进行二级分区的创建和删除，修改仅支持设置是否在线，包含一个内部界面类，用于进行属性设定
 */
public class TableSubPartitionManager extends SQLObjectEditor<TableSubPartition, BaseTablePhysical> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_SAVE_IMMEDIATELY;
	}

	@Override
	protected TableSubPartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
        return new TableSubPartition((BaseTablePhysical)container,true,"");
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TableSubPartition, BaseTablePhysical>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		// 表存在时，禁用二级分区操作
		if (command.getObject().getParentObject().isPersisted()) {
            throw new DBCException("Can't create sub partition on existed table");
		}
	}

    @Override
    public boolean canDeleteObject(TableSubPartition object) {
        // 不允许对二级分区进行删除操作
        return !object.getParentObject().isPersisted();
    }

    @Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			SQLObjectEditor<TableSubPartition, BaseTablePhysical>.ObjectDeleteCommand command,
			Map<String, Object> options) throws DBCException {
		// 不允许对二级分区进行删除操作
		if (command.getObject().getParentObject().isPersisted()) {
            throw new DBCException("Can't delete sub partition on existed table");
		}
		// 若是新增表情况时则直接将改对象从缓存中剔除
		else {
			command.getObject().getParentObject().subPartitionCache.removeObject(command.getObject(), true);
		}
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList,
			SQLObjectEditor<TableSubPartition, BaseTablePhysical>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		// 当表存在时才可进行修改 action
		final String onlineKey = "online";
		if (command.getObject().getParentObject().isPersisted() == true && command.getProperty(onlineKey) != null) {
			StringBuilder sql = new StringBuilder("ALTER TABLE ");
			sql.append(command.getObject().getParentObject().getName());
			sql.append(" SET SUBPARTITION ");
			sql.append(command.getObject().getName());
			sql.append((boolean) command.getProperty("online") ? " ONLINE" : " OFFLINE");

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add subpartition sql: " + sql.toString());
			actionList.add(new SQLDatabasePersistAction("Alter Partition", sql.toString()));
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, TableSubPartition> getObjectsCache(TableSubPartition object) {
		return object.getParentObject().subPartitionCache;
	}
}
