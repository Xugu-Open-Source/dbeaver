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

import java.util.List;
import java.util.Map;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Udt;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

/**
 * 自定义类型管理器，进行自定义类型的创建和删除，不支  持修改
 * 
 * @author Xugu
 */
public class UdtManager extends SQLObjectEditor<Udt, Schema> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("类型名称不能为空");
		}
	}

	@Override
	public DBSObjectCache<? extends DBSObject, Udt> getObjectsCache(Udt object) {
		return object.getSchema().udtCache;
	}
	
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand, Map<String, Object> options)
    {
    	try {
			addObjectChangeActions(monitor,executionContext, actionList, objectChangeCommand,options);
		} catch (DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }


	@Override
	protected Udt createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) throws DBException {
		Schema schema = (Schema) container;
		return new UITask<Udt>() {
			@Override
			protected Udt runTask() {
				EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.TYPE);
				if (!page.edit()) {
					return null;
				}

				Udt udt = new Udt(schema, page.getEntityName());
				udt.setTypeName(page.getEntityName());
				udt.setObjectDefinitionText(
						"CREATE TYPE " + schema.getName() + "." + page.getEntityName() + " AS OBJECT");
				udt.setExtendedDefinitionText(
						"-- CREATE TYPE BODY " + schema.getName() + "." + page.getEntityName() + " AS ");
				udt.setValid(true);
				return udt;
			}
		}.execute();
	}
	
	
	protected  void addObjectChangeActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Udt, Schema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		Udt udt = command.getObject();
		String sql = null;
		sql = udt.getObjectDefinitionText(monitor, options);
		String bodyDefine = udt.getExtendedDefinitionText(monitor);
		if (!(bodyDefine == null || bodyDefine.trim().isEmpty())) {
			sql += "\n" + bodyDefine;
		}
		log.debug("[" + OemConfig.COMPANY_NAME + "] Construct create UDT sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create UDT", sql));
	}

	

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Udt, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		Udt udt = command.getObject();
		String sql = null;
		sql = udt.getObjectDefinitionText(monitor, options);
		String bodyDefine = udt.getExtendedDefinitionText(monitor);
		if (!(bodyDefine == null || bodyDefine.trim().isEmpty())) {
			sql += "\n" + bodyDefine;
		}
		log.debug("[" + OemConfig.COMPANY_NAME + "] Construct create UDT sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create UDT", sql));
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<Udt, SQLObjectEditor<Udt, Schema>.PropertyHandler> command, Map<String, Object> options)
			throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON OBJECT ");
			desc.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));

			log.debug("[" + OemConfig.COMPANY_NAME + "] Construct add udt comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment udt", desc.toString()));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Udt, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP TYPE " + command.getObject().getParentObject().getName() + "."
				+ DBUtils.getQuotedIdentifier(command.getObject());

		log.debug("[" + OemConfig.COMPANY_NAME + "] Construct drop UDT sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop UDT", sql));
	}
}
