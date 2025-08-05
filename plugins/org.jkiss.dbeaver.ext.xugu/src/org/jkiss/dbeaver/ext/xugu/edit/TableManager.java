/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLStructEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * 表、视图管理器，进行表、视图的创建，修改和删除
 */
public class TableManager extends SQLTableManager<Table, Schema> implements DBEObjectRenamer<Table> {
	private static final Class<? extends DBSObject>[] CHILD_TYPES = CommonUtils.array(
			TableColumn.class,
			TableConstraint.class,
			TableForeignKey.class,
			TableIndex.class
	);

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Table> getObjectsCache(Table object) {
		return (DBSObjectCache) object.getSchema().tableCache;
	}

	/**
	 * 在打开新建表窗口前的准备
	 * @throws DBException 
	 */
	@Override
	protected Table createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container,
			Object from, Map<String, Object> options) throws DBException {
		final Table table;
		Schema schema = (Schema) container;
		if (from instanceof DBSEntity) {
			table = new Table(monitor, schema, (DBSEntity) from);
			table.setName(getNewChildName(monitor, schema, ((DBSEntity) from).getName()));
		} else if (from == null) {
            table = new Table(schema, "");
            setNewObjectName(monitor, schema, table);
		} else {
			throw new DBException("无法依据 '" + from + "' 创建表");
		}
		return table;
	}

	@Override
	protected void setNewObjectName(DBRProgressMonitor monitor, Schema parent, Table table) {
		table.setName(getNewChildName(monitor, parent, "NEWTABLE"));
	}

	@Override
	protected void addStructObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLStructEditor<Table, Schema>.StructCreateCommand command,
			Map<String, Object> options) throws DBException {
		// 重写父类的addStructObjectCreateActions方法
		final Table table = command.getObject();
		final NestedObjectCommand tableProps = command.getObjectCommands().get(table);
		if (tableProps == null) {
			log.warn("Object change command not found");
			return;
		}
		final String tableName = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true)
				? table.getFullyQualifiedName(DBPEvaluationContext.DDL)
				: DBUtils.getQuotedIdentifier(table);
		final String slComment = SQLUtils.getDialectFromObject(table).getSingleLineComments()[0];
		final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
		StringBuilder createQuery = new StringBuilder(100);
		createQuery.append("CREATE ").append(getCreateTableType(table)).append(" ").append(tableName).append(" (")
				.append(lineSeparator);
		boolean hasNestedDeclarations = false;
		final Collection<NestedObjectCommand> orderedCommands = getNestedOrderedCommands(command);
		List<DBEPersistAction> otherActions = new ArrayList<>();
		for (NestedObjectCommand nestedCommand : orderedCommands) {
			if (nestedCommand.getObject() == table) {
				continue;
			}
			if (excludeFromDDL(nestedCommand, orderedCommands)) {
				continue;
			}
			if (nestedCommand.getObject() instanceof TableColumn) {
				// 对字段注释做额外处理
				String commentInfo = ((TableColumn) nestedCommand.getObject()).getDescription();
				String realComment = "";
				if (commentInfo != null) {
					realComment = " COMMENT '" + commentInfo + "'";
				}
				final String nestedDeclaration = nestedCommand.getNestedDeclaration(monitor, table, options) + realComment;
				// Insert nested declaration
				if (hasNestedDeclarations) {
					// Check for embedded comment
					int lastLfPos = createQuery.lastIndexOf(lineSeparator);
					int lastCommentPos = createQuery.lastIndexOf(slComment);
					if (lastCommentPos != -1) {
						while (lastCommentPos > 0 && Character.isWhitespace(createQuery.charAt(lastCommentPos - 1))) {
							lastCommentPos--;
						}
					}
					if (lastCommentPos < 0 || lastCommentPos < lastLfPos) {
						createQuery.append(",");
					} else {
						createQuery.insert(lastCommentPos, ",");
					}
					createQuery.append(lineSeparator);
				}
				createQuery.append("\t").append(nestedDeclaration);
				hasNestedDeclarations = true;
				Object isIdenBooleanObject = nestedCommand.getProperty("isIdenBoolean");
				if(isIdenBooleanObject != null && (boolean) isIdenBooleanObject) {
					String string = " identity("+nestedCommand.getProperty("minInteger")+","+nestedCommand.getProperty("stepInteger")+")";
					createQuery.append(string);
				}
			} else {
				// This command should be executed separately
				DBEPersistAction[] nestedActions = nestedCommand.getPersistActions(monitor, executionContext, options);
				if (nestedActions != null && nestedActions.length > 0) {
					Collections.addAll(otherActions, nestedActions);
				}
			}
		}
		createQuery.append(lineSeparator).append(")");
		appendTableModifiers(monitor, table, tableProps, createQuery, false,options);
		// 再额外对分区逻辑进行处理
		Collection<TablePartition> partList = command.getObject().getPartitions(monitor);
		Collection<TableSubPartition> subpartList = command.getObject().getSubPartitions(monitor);
		String tableDef = createQuery.toString();
		if (partList != null && partList.size() != 0) {
			boolean isHashPartition = false;
			Iterator<TablePartition> iterator = partList.iterator();
			boolean isFirstCycle = true;
			String oldName = "";
			while (iterator.hasNext()) {
				TablePartition part = iterator.next();
				// 第一次循环设置分区类型和分区键
				if (isFirstCycle) {
					// hash分区仅需要设置头部，设置好后跳出循环
					if ("HASH".equals(part.getPartiType())) {
						isHashPartition = true;
						tableDef += "\nPARTITION BY " + part.getPartiType() + "(" + part.getPartiKey() + ") PARTITIONS "
								+ table.getPartiNum();
						break;
					} else if ("AUTOMATIC".equals(part.getPartiType())) {
						tableDef += "\nPARTITION BY " + "RANGE(" + part.getPartiKey() + ") INTERVAL "
								+ part.getAutoPartiSpan() + " " + part.getAutoPartiType() + " PARTITIONS(";
					} else {
						tableDef += "\nPARTITION BY " + part.getPartiType() + "(" + part.getPartiKey()
								+ ") PARTITIONS(";
					}
					oldName = part.getName();
				}
				// 非重复缓存处理
				final boolean isNeedAddPartition = isFirstCycle || !oldName.equals(part.getName());
				if (isNeedAddPartition && !part.isSubPartition()) {
					if ("LIST".equals(part.getPartiType())) {
						if("othervalues".equalsIgnoreCase(part.getPartiValue())) {
							tableDef += "\n" + part.getName() + " VALUES(" + part.getPartiValue() + ")";
						}else {
							tableDef += "\n" + part.getName() + " VALUES('" + part.getPartiValue() + "')";
						}
					} else {
						tableDef += "\n" + part.getName() + " VALUES LESS THAN(" + part.getPartiValue() + ")";
					}
					tableDef += ",";
					oldName = part.getName();
				}
				isFirstCycle = false;
			}
			if (!isHashPartition) {
				tableDef = tableDef.substring(0, tableDef.length() - 1);
				tableDef += "\n)";
			}
			if (subpartList != null && subpartList.size() != 0) {
				boolean isHashSubPartition = false;
				Iterator<TableSubPartition> iterator2 = subpartList.iterator();
				isFirstCycle = true;
				while (iterator2.hasNext()) {
					TableSubPartition part = iterator2.next();
					// 第一次循环设置分区类型和分区键
					if (isFirstCycle) {
						// hash分区仅需要设置头部，设置好后跳出循环
						if ("HASH".equals(part.getPartiType())) {
							isHashSubPartition = true;
							tableDef += "\nSUBPARTITION BY " + part.getPartiType() + "(" + part.getPartiKey()
									+ ") SUBPARTITIONS " + table.getSubpartiNum();
							break;
						} else {
							tableDef += "\nSUBPARTITION BY " + part.getPartiType() + "(" + part.getPartiKey()
									+ ") SUBPARTITIONS(";
						}
						oldName = part.getName();
					}
					// 非重复缓存处理
					final boolean isNeedAddPartition = isFirstCycle || !oldName.equals(part.getName());
					if (isNeedAddPartition && part.isSubPartition()) {
						if ("LIST".equals(part.getPartiType())) {
							tableDef += "\n" + part.getName() + " VALUES('" + part.getPartiValue() + "')";
						} else {
							tableDef += "\n" + part.getName() + " VALUES LESS THAN(" + part.getPartiValue() + ")";
						}
						tableDef += ",";
						oldName = part.getName();
					}
					isFirstCycle = false;
				}

				if (!isHashSubPartition) {
					tableDef = tableDef.substring(0, tableDef.length() - 1);
					tableDef += "\n)";
				}
			}
			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create table sql: " + tableDef);
		}
		actions.add(new SQLDatabasePersistAction("Create table", tableDef));
		actions.addAll(otherActions);
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Table, Schema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperties().size() > 1 || command.getProperty(commentKey) == null) {
			StringBuilder query = new StringBuilder("ALTER TABLE ");
			query.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");
			appendTableModifiers(monitor, command.getObject(), command, query, true,options);

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter table sql: " + query.toString());
			actionList.add(new SQLDatabasePersistAction(query.toString()));
			Schema schema = command.getObject().getSchema();
			try {
				schema.tableCache.refreshObject(monitor, schema, command.getObject());
			} catch (DBException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<Table, SQLObjectEditor<Table, Schema>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			String sql = "COMMENT ON TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)
					+ " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getComment());
			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add table comment sql: " + sql);
			actions.add(new SQLDatabasePersistAction("Comment table", sql));
		}
	}

	@Override
	protected void appendTableModifiers(
			DBRProgressMonitor monitor,
			Table table,
			NestedObjectCommand tableProps,
			StringBuilder ddl,
			boolean alter,
			Map<String, Object> options) {
		final String tableSpaceKey = "tablespace";
		if (tableProps.getProperty(tableSpaceKey) != null) {
			String delimiter = getDelimiter(options);
			Object tablespace = table.getTablespace();
			if (tablespace instanceof Tablespace) {
				if (table.isPersisted()) {
					ddl.append(delimiter).append("\nMOVE TABLESPACE ").append(((Tablespace) tablespace).getName());
				} else {
					ddl.append(delimiter).append("\nTABLESPACE ").append(((Tablespace) tablespace).getName());
				}
			}
		}
	}

	/**
	 * 修改表名、视图名
	 */
	@Override
	protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Table, Schema>.ObjectRenameCommand command,
			Map<String, Object> options) {
		String sql = "ALTER TABLE " + DBUtils.getQuotedIdentifier(command.getObject().getSchema()) + "."
				+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getOldName()) + " RENAME TO "
				+ DBUtils.getQuotedIdentifier(command.getObject().getDataSource(), command.getNewName());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct rename table sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Rename table", sql));
	}

	/**
	 * 删除表、视图
	 */
	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Table, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		Table object = command.getObject();
		String sql = "DROP " + (object.isView() ? "VIEW" : "TABLE") + " "
				+ object.getFullyQualifiedName(DBPEvaluationContext.DDL)
				+ (!object.isView() && CommonUtils.getOption(options, OPTION_DELETE_CASCADE) ? " CASCADE" : "");

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop table sql: " + sql);
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_table, sql));
	}

	@NotNull
	@Override
	public Class<? extends DBSObject>[] getChildTypes() {
		return CHILD_TYPES;
	}

	@Override
	public void renameObject(DBECommandContext commandContext, Table object, Map<String, Object> options,
			String newName) throws DBException {
		processObjectRename(commandContext, object, options, newName);
	}

	@Override
	public Collection<? extends DBSObject> getChildObjects(DBRProgressMonitor monitor, Table object,
			Class<? extends DBSObject> childType) throws DBException {
        if (childType == TableColumn.class) {
            return object.getAttributes(monitor);
        } else if (childType == TableConstraint.class) {
            return object.getConstraints(monitor);
        } else if (childType == TableForeignKey.class) {
            return object.getAssociations(monitor);
        } else if (childType == TableIndex.class) {
            return object.getIndexes(monitor);
        }
        return null;
	}
}
