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
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.View;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视图管理器，进行视图的创建和删除，修改相当于创建并替换
 */
public class ViewManager extends SQLObjectEditor<View, Schema> implements DBEObjectRenamer<View> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("View name cannot be empty");
		}
		if (CommonUtils.isEmpty(command.getObject().getViewText())) {
			throw new DBException("View definition cannot be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, View> getObjectsCache(View object) {
		return (DBSObjectCache<? extends DBSObject, View>) object.getSchema().viewCache;
	}

	@Override
	protected View createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) throws DBException {
		final View view;
		Schema schema = (Schema) container;
		if (from instanceof View) {
			view = new View(monitor, schema, (View) from);
			view.setName(getNewChildName(monitor, schema, ((DBSEntity) from).getName()));
		} else if (from == null) {
            view = new View(schema, getNewChildName(monitor, schema, "new_view"));
		} else {
			throw new DBException("无法依据 '" + from + "' 创建视图");
		}
		return view;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<View, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		createOrReplaceViewQuery(monitor, actions, command);
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<View, Schema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperties().size() > 1 || command.getProperty(commentKey) == null) {
			createOrReplaceViewQuery(monitor, actionList, command);
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<View, SQLObjectEditor<View, Schema>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON VIEW ");
			desc.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add view comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment View", desc.toString()));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<View, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop view sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop view", sql));
	}
	
	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<View, Schema>.ObjectRenameCommand command,
			Map<String, Object> options) {
		// 删除原始视图
		String dropSql = "DROP VIEW " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "." +
				DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName());
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop view sql: " + dropSql);
		actions.add(new SQLDatabasePersistAction("Drop view", dropSql));

		// 创建新视图
		String oldCreateSql = command.getObject().getViewText();
		Pattern pattern = Pattern.compile("VIEW\\s+\\S+\\s+AS", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(oldCreateSql);
		String oldViewName;
		if (matcher.find()) {
			oldViewName = matcher.group();
		} else {
			throw new IllegalStateException("视图名表达式匹配失败");
		}
		oldViewName = oldViewName.substring(4, oldViewName.length()-2).trim();
		String newViewName = DBUtils.getQuotedIdentifier(command.getObject().getSchema()) +"." +
				DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName());
		String newCreateSql = oldCreateSql.replace(oldViewName, newViewName);
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create view sql: " + dropSql);
		actions.add(new SQLDatabasePersistAction("Create view", newCreateSql));
	}

	private void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
			DBECommandComposite<View, PropertyHandler> command) {
		View view = command.getObject();

		view.setViewText(view.getViewText());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create view sql: " + view.getViewText());
		actions.add(0, new SQLDatabasePersistAction("Create view", view.getViewText()));
	}


	@Override
	public void renameObject(DBECommandContext commandContext, View object, Map<String, Object> options, String newName)
			throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}
}
