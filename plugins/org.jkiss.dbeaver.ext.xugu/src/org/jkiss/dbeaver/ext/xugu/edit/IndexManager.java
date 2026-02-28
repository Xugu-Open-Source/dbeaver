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
import org.jkiss.dbeaver.ext.xugu.model.BaseTablePhysical;
import org.jkiss.dbeaver.ext.xugu.model.TableColumn;
import org.jkiss.dbeaver.ext.xugu.model.TableConstraint;
import org.jkiss.dbeaver.ext.xugu.model.TableConstraintColumn;
import org.jkiss.dbeaver.ext.xugu.model.TableIndex;
import org.jkiss.dbeaver.ext.xugu.model.TableIndexColumn;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 索引管理器， 进行索引的增加和删除
 */
public class IndexManager extends SQLIndexManager<TableIndex, BaseTablePhysical> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, TableIndex> getObjectsCache(TableIndex object) {
		return object.getParentObject().getSchema().indexCache;
	}

	@Override
	protected TableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		BaseTablePhysical table = (BaseTablePhysical) container;

		return new TableIndex(table.getSchema(), table, "INDEX", true, DBSIndexType.UNKNOWN);
	}

	/**
	 * 重新组装创建 index 语句，增加 local 关键字
	 */
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		final BaseTablePhysical table = command.getObject().getTable();
		final TableIndex index = command.getObject();
		Collection<TableConstraint> constraints;
		// 若约束中已有当前索引包含的列，则跳过索引创建，约束将会隐式创建索引
		try {
			constraints = table.getConstraints(monitor);
			if (constraints != null) {
				List<TableColumn> indexTableCols = index.getColumns().stream()
						.map(TableIndexColumn::getTableColumn)
						.collect(Collectors.toList());

				for (TableConstraint cons : constraints) {
					List<TableConstraintColumn> consCols = cons.getColumns();
					List<TableColumn> consTableCols;
					if (consCols == null) {
						continue;
					} else {
						consTableCols = consCols.stream()
							.map(TableConstraintColumn::getAttribute)
							.collect(Collectors.toList());
						if (consTableCols.equals(indexTableCols)) {
							return;
						}
					}
				}
			}
		} catch (DBException e) {
			throw new IllegalStateException(e);
		}
		// Create index
		final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
		index.setName(indexName);
		final String tableName = DBUtils.getEntityScriptName(table, options);
		StringBuilder decl = new StringBuilder(40);
		decl.append("CREATE");
		appendIndexModifiers(index, decl);
		decl.append(" INDEX ").append(indexName);
		decl.append(" ON ").append(tableName).append(" (");
		// Get columns using void monitor
		boolean firstColumn = true;
		for (DBSTableIndexColumn indexColumn : CommonUtils
				.safeCollection(command.getObject().getAttributeReferences(new VoidProgressMonitor()))) {
			if (!firstColumn) {
				decl.append(",");
			}
			firstColumn = false;
			decl.append(DBUtils.getQuotedIdentifier(indexColumn));
			appendIndexColumnModifiers(monitor, decl, indexColumn);
		}
		decl.append(")");
		appendIndexType(index, decl);

		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString()));

		actions.remove(0);
		// 局部或全局索引
		if (index.isLocal()) {
			decl.append(" LOCAL");
		} else {
			decl.append(" GLOBAL");
		}
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString()));
	}

	/**
	 * 重写虛谷索引数据类型语法
	 */
	@Override
	protected void appendIndexType(TableIndex index, StringBuilder decl) {
		decl.append(" INDEXTYPE IS ");
		decl.append(index.getIndexType().getName());
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_index,
				getDropIndexPattern(command.getObject())
						.replace(PATTERN_ITEM_TABLE,
								command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
						.replace(PATTERN_ITEM_INDEX, command.getObject().getName())
						.replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject()))));
		String t = getDropIndexPattern(command.getObject())
				.replace(PATTERN_ITEM_TABLE,
						command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
				.replace(PATTERN_ITEM_INDEX, command.getObject().getName())
				.replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject()));
	}

	@Override
	protected String getDropIndexPattern(TableIndex index) {
		return "DROP INDEX " + PATTERN_ITEM_TABLE + "." + PATTERN_ITEM_INDEX;
	}

	@Override
	protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, BaseTablePhysical owner,
			DBECommandAbstract<TableIndex> command, Map<String, Object> options) {
		return null;
	}
}
