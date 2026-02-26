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
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.ObjectType;
import org.jkiss.dbeaver.ext.xugu.model.ObjectValidateAction;
import org.jkiss.dbeaver.ext.xugu.model.Table;
import org.jkiss.dbeaver.ext.xugu.model.Trigger;
import org.jkiss.dbeaver.ext.xugu.model.View;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;
/**
 * 触发器管理器，进行触发器的创建和删除，修改相当于创建并替换，包含一个内部界面类，用于进行属性设定
 */
public class TriggerManager extends SQLTriggerManager<Trigger, BaseTable> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Trigger> getObjectsCache(Trigger object) {
		return object.getTable().triggerCache;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Trigger name cannot be empty");
		}
	}

	@Override
	public boolean canCreateObject(Object container) {
        // 只能在表或者视图对象上新建DML触发器，否则createDatabaseObject无法拿到表对象抛空指针，之前ui界面遍历不能出现在此模块
		return container instanceof Table || container instanceof View;
	}

	@Override
	protected Trigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		return new Trigger((BaseTable) container,"NewTrigger");
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Trigger, BaseTable>.ObjectCreateCommand command,
			Map<String, Object> options) {
		createOrReplaceTriggerQuery(actions, command.getObject());
	}

	@Override
	protected void createOrReplaceTriggerQuery(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, Trigger trigger, boolean create) {
		// TODO 创建或替换触发器查询
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Trigger, BaseTable>.ObjectChangeCommand command,
			Map<String, Object> options) {
		final String objectDefKey = "objectDefinitionText";
		final String triggerConditionKey = "triggerCondition";

		if (command.getProperty(objectDefKey) != null || command.getProperty(triggerConditionKey) != null) {
			createOrReplaceTriggerQuery(actionList, command.getObject());
		}
	}

	/**
	 * 修改模式名称
	 */
	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Trigger, BaseTable>.ObjectRenameCommand command,
			Map<String, Object> options) {
		StringBuilder desc = new StringBuilder(100);
		if (!CommonUtils.isEmpty(command.getNewName())) {
			desc.append("ALTER TRIGGER ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()));
			desc.append(" RENAME TO ");
			desc.append(DBUtils.getQuotedIdentifier(command.getObject().getDataSource(),
					command.getNewName().toUpperCase()));
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct rename schema sql: " + desc.toString());
		actionList.add(new SQLDatabasePersistAction("rename schema", desc.toString()));
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<Trigger, SQLObjectEditor<Trigger, BaseTable>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON TRIGGER ");
			desc.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add trigger comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment Trigger", desc.toString()));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Trigger, BaseTable>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop trigger sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop trigger", sql));
	}

	protected void createOrReplaceTriggerQuery(List<DBEPersistAction> actions, Trigger trigger) {
		String source = Utils.normalizeSourceName(trigger, false);
		try {
			source = trigger.getExtendedDefinitionText(new VoidProgressMonitor());
		} catch (DBException e) {
			log.error(e);
		}
		final String codeBody = "\nBEGIN\n\nEND";
		if (source == null || !Utils.checkString(source) || source.equals(codeBody)) {
			actions.add(new ObjectValidateAction(trigger, ObjectType.TRIGGER, "Create trigger action", source));
		} else {
			String event = trigger.getTriggeringEvent();
			String timing = trigger.getTriggerTime();
			String type = trigger.getTriggerType();
			List<String> includeCols = trigger.getIncludeColumns();
			String targetCols = "";
			if (includeCols != null) {
				for (String str : includeCols) {
					targetCols += "\"" + str + "\"" + ",";
				}
				if (!"".equals(targetCols)) {
					targetCols = targetCols.substring(0, targetCols.length() - 1);
					targetCols = " OF " + targetCols;
				}
			}
			// 处理触发器事件字段
			final String separator = ",";
			if (event.indexOf(separator) != -1) {
				event = event.replaceAll(",", " OR ");
			}
			String condition = trigger.getTriggerCondition();
			String realCondition = condition != null ? "".equals(condition) ? null : condition : null;
			source = "CREATE OR REPLACE TRIGGER " + trigger.getFullyQualifiedName(DBPEvaluationContext.DDL) + " \n"
					+ timing + " " + event + targetCols + " ON "
					+ trigger.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " \n" + type
                    + ("FOR EACH ROW".equals(trigger.getTriggerType()) && !"INSTEAD OF".equals(timing)
                    ? " WHEN(" + realCondition + ") \n"
                    : " \n")
					+ source;

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create trigger sql: " + source);
			actions.add(new SQLDatabasePersistAction("Create trigger", source, true));
			// trigger.setPersisted(true);
		}
	}
}
