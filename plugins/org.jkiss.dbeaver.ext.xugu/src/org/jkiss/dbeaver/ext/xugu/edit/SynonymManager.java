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
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Synonym;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * 同义词管理器，进行同义词的创建和删除，不支持修改，包含一个内部界面类，用于进行属性设定
 */
public class SynonymManager extends SQLObjectEditor<Synonym, Schema> implements DBEObjectRenamer<Synonym> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("同义词名称不能为空");
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, Synonym> getObjectsCache(Synonym object) {
		return object.getSchema().synonymCache;
	}

	@Override
	protected Synonym createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) throws DBException {
        if (container instanceof DataSource dataSource) {
            return new Synonym(dataSource.publicSchema, "");
        } else {
            return new Synonym((Schema) container, "");
        }
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Synonym, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		addCreateActions(monitor, executionContext, actions, command, options);
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Synonym, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		addDeleteActions(monitor, executionContext, actions, command, options);
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Synonym, Schema>.ObjectRenameCommand command,
			Map<String, Object> options) {
		command.getObject().setName(command.getOldName());
		addDeleteActions(monitor, executionContext, actions, command, options);
		command.getObject().setName(command.getNewName());
		addCreateActions(monitor, executionContext, actions, command, options);
	}

	private void addCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<Synonym> command,
			Map<String, Object> options) {
		Synonym synonym = command.getObject();
		String sql = "CREATE ";
		if (synonym.isPublic()) {
			sql += "PUBLIC SYNONYM " + synonym.getName() + " FOR " + synonym.getTargetName();
		} else {
			sql += "SYNONYM " + synonym.getParentObject().getName() + "." + synonym.getName() + " FOR "
					+ synonym.getParentObject().getName() + "." + synonym.getTargetName();
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create synonym sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create synonym", sql));
	}

	private void addDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, DBECommand<Synonym> command,
			Map<String, Object> options) {
		Synonym synonym = command.getObject();
		String sql = "DROP ";
		if (synonym.isPublic()) {
			sql += "PUBLIC SYNONYM " + DBUtils.getQuotedIdentifier(synonym);
		} else {
			sql += "SYNONYM " + synonym.getParentObject().getName() + "." + DBUtils.getQuotedIdentifier(synonym);
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop synonym sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop synonym", sql));
	}
	
	@Override
	public void renameObject(DBECommandContext commandContext, Synonym object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}
}
