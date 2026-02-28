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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.ObjectStatus;
import org.jkiss.dbeaver.ext.xugu.model.TableForeignKey;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * 外键管理器，进行外键的增加
 */
public class ForeignKeyManager extends SQLForeignKeyManager<TableForeignKey, BaseTable> {
	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, TableForeignKey> getObjectsCache(TableForeignKey object) {
		return object.getParentObject().getSchema().foreignKeyCache;
	}

	@Override
	protected TableForeignKey createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		BaseTable table = (BaseTable) container;
        TableForeignKey foreignKey = new TableForeignKey(table,
                "",
                ObjectStatus.ENABLED,
                null,
                DBSForeignKeyModifyRule.NO_ACTION,
                DBSForeignKeyModifyRule.NO_ACTION);
        foreignKey.setName(getNewConstraintName(monitor, foreignKey));
        return foreignKey;
    }

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		TableForeignKey foreignKey = (TableForeignKey) command.getObject();
		BaseTable table = command.getObject().getTable();

		StringBuilder decl = new StringBuilder(100);
		decl.append("ALTER TABLE ");
		decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
		decl.append(" ADD ");
		decl.append(getNestedDeclaration(monitor, table, command, options));
		decl.append(";");
		decl.append(CommonUtils.getLineSeparator());

		decl.append("ALTER TABLE ");
		decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
		decl.append(foreignKey.isEnable() ? " ENABLE" : " DISABLE");
		decl.append(" CONSTRAINT ");
		decl.append(foreignKey.getName());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create foreign key sql: " + decl.toString());
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_foreign_key, decl.toString()));
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<TableForeignKey, BaseTable>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		TableForeignKey fk = (TableForeignKey) command.getObject();
		BaseTable table = fk.getTable();
		String sql = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL)
				+ (fk.isEnable() ? " ENABLE" : " DISABLE") + " CONSTRAINT " + fk.getName();

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter foreign key sql: " + sql);
		actionList.add(new SQLDatabasePersistAction("Alter foreign key", sql));
	}
}
