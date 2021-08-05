/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.ext.xugu.model.Package;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 包管理器，进行包的创建和删除（修改等同于创建并替换）
 */
public class PackageManager extends SQLObjectEditor<Package, Schema> {
	private final static Pattern PATTERN_OR = Pattern.compile("(OR)", Pattern.CASE_INSENSITIVE);
	private final static Pattern PATTERN_PACKAGE = Pattern.compile("(PACKAGE)", Pattern.CASE_INSENSITIVE);

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Package name cannot be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Package> getObjectsCache(Package object) {
		return object.getSchema().packageCache;
	}

	@Override
	protected Package createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		Schema schema = (Schema) container;
		return new UITask<Package>() {
			@Override
			protected Package runTask() {
				EntityEditPage editPage = new EntityEditPage(schema.getDataSource(), DBSEntityType.PACKAGE);
				if (!editPage.edit()) {
					return null;
				}
				String packName = editPage.getEntityName();
				Package pkg = new Package(schema, packName);
				pkg.setObjectDefinitionText("CREATE OR REPLACE PACKAGE " + schema.getName() + "." + packName + "\n"
						+ "AS\n" + "-- Package header\n" + "END;");
				pkg.setExtendedDefinitionText("CREATE OR REPLACE PACKAGE BODY " + schema.getName() + "." + packName
						+ "\n" + "AS\n" + "-- Package body\n" + "END;");
				pkg.setValid(true);
				return pkg;
			}
		}.execute();
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand objectCreateCommand, Map<String, Object> options)
			throws DBException {
		createOrReplaceProcedureQuery(actions, objectCreateCommand.getObject());
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		final String objectDefKey = "objectDefinitionText";
		final String extendedDefKey = "extendedDefinitionText";
		if (command.getProperty(objectDefKey) != null || command.getProperty(extendedDefKey) != null) {
			createOrReplaceProcedureQuery(actionList, command.getObject());
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<Package, SQLObjectEditor<Package, Schema>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON PACKAGE ");
			desc.append(command.getObject().getSchema().getName() + "." + command.getObject().getName());
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add package comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment Package", desc.toString()));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand objectDeleteCommand, Map<String, Object> options) {
		final Package object = objectDeleteCommand.getObject();
		String sql = "DROP PACKAGE " + object.getFullyQualifiedName(DBPEvaluationContext.DDL);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop package sql: " + sql.toString());
		actions.add(new SQLDatabasePersistAction("Drop package", sql));
	}

	private void createOrReplaceProcedureQuery(List<DBEPersistAction> actionList, Package pack) {
		try {
			String header = pack.getObjectDefinitionText(new VoidProgressMonitor(), DBPScriptObject.EMPTY_OPTIONS);
			// 对 header 进行预处理，强制增加 CREATE OR REPLACE 关键字
			Matcher m1 = PATTERN_OR.matcher(header);
			String keyWord1 = "OR";

			Matcher m2 = PATTERN_PACKAGE.matcher(header);
			String keyWord2 = "PACKAGE";
			if (m1.find()) {
				keyWord1 = m1.group(0);
			}
			if (m2.find()) {
				keyWord2 = m2.group(0);
			}
			if (header.indexOf(keyWord1) == -1) {
				header = "CREATE OR REPLACE PACKAGE " + header.substring(header.indexOf(keyWord2) + 8);
			}
			if (!CommonUtils.isEmpty(header)) {
				log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create package header sql: " + header);
				actionList.add(new ObjectValidateAction(pack, ObjectType.PACKAGE, "Create package header", header));
			}
			String body = pack.getExtendedDefinitionText(new VoidProgressMonitor());
			// 对 body 进行预处理，强制增加 CREATE OR REPLACE 关键字
			Matcher m3 = PATTERN_OR.matcher(body);
			Matcher m4 = PATTERN_PACKAGE.matcher(body);
			if (m3.find()) {
				keyWord1 = m3.group(0);
			}
			if (m4.find()) {
				keyWord2 = m4.group(0);
			}
			if (body.indexOf(keyWord1) == -1) {
				body = "CREATE OR REPLACE PACKAGE " + body.substring(body.indexOf(keyWord2) + 8);
			}
			if (!CommonUtils.isEmpty(body)) {
				log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create package body sql: " + body);
				actionList.add(new ObjectValidateAction(pack, ObjectType.PACKAGE_BODY, "Create package body", body));
			} else {
				String sql = "DROP PACKAGE BODY " + pack.getFullyQualifiedName(DBPEvaluationContext.DDL);

				log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop package body sql: " + sql);
				actionList.add(
						new SQLDatabasePersistAction("Drop package body", sql, DBEPersistAction.ActionType.OPTIONAL));
			}
		} catch (DBException e) {
			log.warn(e);
		}
	}
}
