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
import org.jkiss.dbeaver.ext.xugu.model.DataType;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.TableColumn;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableColumnManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import net.sf.jsqlparser.statement.alter.Alter;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 表字段管理器，进行字段的创建，修改和删除（均相当于修改表结构）
 */
public class TableColumnManager extends SQLTableColumnManager<TableColumn, BaseTable>
		implements DBEObjectRenamer<TableColumn> {
	String first = null;

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, TableColumn> getObjectsCache(TableColumn object) {
		return object.getParentObject().getContainer().tableCache.getChildrenCache(object.getParentObject());
	}

	@Override
	protected ColumnModifier[] getSupportedModifiers(TableColumn column, Map<String, Object> options) {
		return new ColumnModifier[] { DataTypeModifier, DefaultModifier, NullNotNullModifierConditional };
	}

	@Override
	protected TableColumn createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		BaseTable parent = (BaseTable) container;

		DBSDataType columnType = findBestDataType(parent.getDataSource(), "varchar2");

		final TableColumn column = new TableColumn(parent);
		column.setName(getNewColumnName(monitor, context, parent));
		column.setDataType((DataType) columnType);
		column.setTypeName(columnType == null ? "INTEGER" : columnType.getName());
		column.setMaxLength(columnType != null && columnType.getDataKind() == DBPDataKind.STRING ? 100 : 0);
		column.setValueType(columnType == null ? Types.INTEGER : columnType.getTypeID());
		column.setOrdinalPosition(-1);
		return column;
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<TableColumn, BaseTable>.ObjectCreateCommand command,
			Map<String, Object> options) {
		final String commentKey = "comment";
		final BaseTable table = command.getObject().getTable();
		String query;
		String[] defaults = null;

		if (getNestedDeclaration(monitor, table, command, options).toString() != null) {
			defaults = getNestedDeclaration(monitor, table, command, options).toString().split(" DEFAULT ");
		}
		DBPDataKind dataKind = command.getObject().getDataKind();
		if (dataKind == DBPDataKind.STRING && defaults.length > 1) {
			query = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD " + defaults[0]
					+ " DEFAULT " + "'" + defaults[1] + "'";
		} else {
			query = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + " ADD "
					+ getNestedDeclaration(monitor, table, command, options);
		}
		if (command.getProperty(commentKey) != null && !"".equals(command.getProperty(commentKey))) {
			query += " COMMENT '" + command.getObject().getComment(monitor) + "'";
		}

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create table column sql: " + query);
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_table_column, query));
		try {
			table.getSchema().tableCache.refreshObject(monitor, table.getSchema(), table);
		} catch (DBException e) {
			log.error(e);
		}
	}

	/**
	 * 修改了表结构修改的 SQL 语句
	 * 
	 * @param monitor          进程监视器
	 * @param executionContext 执行上下文
	 * @param actionList       动作列表
	 * @param command          命令
	 * @param options          可选项键值对
	 * @throws DBException 数据库异常
	 */
	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<TableColumn, BaseTable>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		final TableColumn column = command.getObject();
		// 遍历 properties，确定每一项新更改均不为空才进行 action 添加
		if (command.getProperties().size() > 0) {
			Map<Object, Object> props = command.getProperties();
			Collection<Object> propKeys = props.keySet();
			Collection<Object> propValues = props.values();
			Iterator<Object> it1 = propKeys.iterator();
			Iterator<Object> it2 = propValues.iterator();
			//记载自增属性的更改数
			int count = 0;
			while (it1.hasNext()) {
				String key = it1.next().toString();
				Object value = it2.next();
				if ("comment".equals(key)) {

					if (CommonUtils.isEmpty(value.toString())) {

					}
					String sql = "COMMENT ON COLUMN "
							+ column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "."
							+ DBUtils.getQuotedIdentifier(column) + " IS '"
							+ column.getComment(new VoidProgressMonitor()) + "'";

					log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter column comment sql: " + sql);
					actionList.add(new SQLDatabasePersistAction("Comment column", sql));
				}else if("samplingInterval".equals(key)) {
					String sql = "call dbms_stat.analyze_table('" + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + "','"+column.getName() 
					+"',"+ value +",null)";
					log.debug("[" + OemConfig.OEM_NAME_EN + "] 设置离散采样间隔: " + sql);
					actionList.add(new SQLDatabasePersistAction(" comment column", sql));
				}else if("isIdenBoolean".equals(key)||"stepInteger".equals(key)||"minInteger".equals(key)){
					if(column.getIsIdenBoolean()){	
						count++;
						if(count<2) {
							String sql = "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
									+ " ALTER COLUMN \" "+column.getName() + "\" " + column.getTypeName() + " identity (" 
									+ (command.getProperty("minInteger")==null?column.getMinInteger().toString():command.getProperty("minInteger"))
									+"," +(command.getProperty("stepInteger")==null?column.getStepInteger().toString():command.getProperty("stepInteger"))+")";	
							log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter column comment sql: " + sql);
							actionList.add(new SQLDatabasePersistAction("Comment column", sql));
						}else {
							continue;						}
						
					}else {
						continue;
					} 
				}
				else {
					String sql = "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
							+ " ALTER COLUMN ";
					switch (key) {
					case "defaultValue":
						if (command.getProperty("defaultValue").equals("")) {
							sql += "\"" + column.getName() + "\"" + " DROP DEFAULT";
						} else {
							sql += "\"" + column.getName() + "\"" + " SET DEFAULT '"
									+ command.getProperty("defaultValue") + "'";
						}
						break;
					case "required":
						if ((boolean) command.getProperty("required")) {
							sql += "\"" + column.getName() + "\"" + " SET NOT NULL";
						} else {
							sql += "\"" + column.getName() + "\"" + " DROP NOT NULL";
						}
						break;
					default:
						sql += getNestedDeclaration(monitor, column.getTable(), command, options);
						break;
					}
					if (first != null && first.equals(sql)) {
						first = "";
						continue;
					} else {
						first = sql;
						log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter table column sql: " + sql);
						actionList.add(new SQLDatabasePersistAction("Modify column", sql));
					}
				}
			}
		}

		try {
			BaseTable table = column.getTable();
			Schema schema = table.getSchema();
			table.getDataSource().schemaCache.refreshObject(monitor, schema.getDataSource(), schema);
		} catch (DBException e) {
			log.error(e);
		}
	}

	@Override
	protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, BaseTable owner,
			DBECommandAbstract<TableColumn> command, Map<String, Object> options) {

		StringBuilder decl = new StringBuilder(100);
		TableColumn column = command.getObject();

		// 需特殊处理数据类型
		List<String> dataTypes = new ArrayList<String>();
		dataTypes.add("INTERVAL DAY TO SECOND");
		dataTypes.add("INTERVAL HOUR TO SECOND");
		dataTypes.add("INTERVAL MINUTE TO SECOND");
		dataTypes.add("INTERVAL YEAR TO MONTH");
		dataTypes.add("INTERVAL DAY TO HOUR");
		dataTypes.add("INTERVAL DAY TO MINUTE");
		dataTypes.add("INTERVAL HOUR TO MINUTE");

		String typeName = column.getTypeName();
		if (dataTypes.contains(typeName)) {
			// 创建列
			String columnName = DBUtils.getQuotedIdentifier(column.getDataSource(), column.getName());

			if (command instanceof SQLObjectEditor.ObjectRenameCommand) {
				columnName = DBUtils.getQuotedIdentifier(column.getDataSource(),
						((ObjectRenameCommand) command).getNewName());
			}

			// 特殊数据类型处理
			decl.append(columnName).append(dataTypeModifier(monitor, column, command));
		} else {
			decl = super.getNestedDeclaration(monitor, owner, command, options);
		}

		return decl;
	}

	protected StringBuilder dataTypeModifier(DBRProgressMonitor monitor, TableColumn column,
			DBECommandAbstract<TableColumn> command) {
		StringBuilder sql = new StringBuilder(50);
		final String typeName = column.getTypeName();
		DBPDataKind dataKind = column.getDataKind();
		final DBSDataType dataType = super.findBestDataType(column.getDataSource(), typeName);

		if (dataType == null) {
			log.debug("Type name '" + typeName + "' is not supported by driver");
		} else {
			dataKind = dataType.getDataKind();
		}

		final String typeDayToSecond = "INTERVAL DAY TO SECOND";
		final String typeHourToSecond = "INTERVAL HOUR TO SECOND";
		final String typeMinuteToSecond = "INTERVAL MINUTE TO SECOND";
		final String typeYearToMonth = "INTERVAL YEAR TO MONTH";
		final String typeDayToHour = "INTERVAL DAY TO HOUR";
		final String typeDayToMinute = "INTERVAL DAY TO MINUTE";
		final String typeHourToMinute = "INTERVAL HOUR TO MINUTE";

		String modifiers = SQLUtils.getColumnTypeModifiers(column.getDataSource(), column, typeName, dataKind);
		if (modifiers == null) {
			sql.append(' ').append(typeName);
			return sql;
		} else if (typeDayToSecond.equals(typeName)) {
			String[] split = CommonUtils.split(modifiers.replace("(", "").replace(")", ""), ",");
			String precision = split[0];
			String scale = split[1];
			sql.append(' ').append("INTERVAL DAY").append("(" + precision + ')').append(" TO SECOND")
					.append("(" + scale + ')');
		} else if (typeHourToSecond.equals(typeName)) {
			String[] split = CommonUtils.split(modifiers.replace("(", "").replace(")", ""), ",");
			String precision = split[0];
			String scale = split[1];
			sql.append(' ').append("INTERVAL HOUR").append("(" + precision + ')').append(" TO SECOND")
					.append("(" + scale + ')');
		} else if (typeMinuteToSecond.equals(typeName)) {
			String[] split = CommonUtils.split(modifiers.replace("(", "").replace(")", ""), ",");
			String precision = split[0];
			String scale = split[1];
			sql.append(' ').append("INTERVAL MINUTE").append("(" + precision + ')').append(" TO SECOND")
					.append("(" + scale + ')');
		} else if (typeYearToMonth.equals(typeName)) {
			sql.append(' ').append("INTERVAL YEAR").append(modifiers).append(" TO MONTH");
		} else if (typeDayToHour.equals(typeName)) {
			sql.append(' ').append("INTERVAL DAY").append(modifiers).append(" TO HOUR");
		} else if (typeDayToMinute.equals(typeName)) {
			sql.append(' ').append("INTERVAL DAY").append(modifiers).append(" TO MINUTE");
		} else if (typeHourToMinute.equals(typeName)) {
			sql.append(' ').append("INTERVAL HOUR").append(modifiers).append(" TO MINUTE");
		}
		return sql;
	};

	@Override
	public void renameObject(DBECommandContext commandContext, TableColumn object, String newName) throws DBException {
		processObjectRename(commandContext, object, newName);
	}

	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<TableColumn, BaseTable>.ObjectRenameCommand command,
			Map<String, Object> options) {
		System.out.println("");
		final TableColumn column = command.getObject();
		String sql = "ALTER TABLE " + column.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL)
				+ " RENAME COLUMN " + DBUtils.getQuotedIdentifier(column.getDataSource(), command.getOldName()) + " TO "
				+ DBUtils.getQuotedIdentifier(column.getDataSource(), command.getNewName());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct rename table column sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Rename column", sql));
	}
}
